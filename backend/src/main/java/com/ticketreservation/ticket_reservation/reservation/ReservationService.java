package com.ticketreservation.ticket_reservation.reservation;

import com.ticketreservation.ticket_reservation.common.exception.CustomException;
import com.ticketreservation.ticket_reservation.common.exception.ErrorCode;
import com.ticketreservation.ticket_reservation.domain.event.Event;
import com.ticketreservation.ticket_reservation.domain.event.EventRepository;
import com.ticketreservation.ticket_reservation.domain.member.Member;
import com.ticketreservation.ticket_reservation.domain.member.MemberRepository;
import com.ticketreservation.ticket_reservation.domain.reservation.Reservation;
import com.ticketreservation.ticket_reservation.domain.reservation.ReservationRepository;
import com.ticketreservation.ticket_reservation.domain.reservation.ReservationStatus;
import com.ticketreservation.ticket_reservation.reservation.dto.ReservationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 선착순 예매의 핵심 동시성 제어 로직.
 *
 * 재고 차감의 "관문(gate)"은 Redis Lua 스크립트(reserve.lua)가 담당한다. Lua 스크립트는
 * Redis 이벤트 루프 안에서 싱글 스레드로 순차 실행되므로, "재고 확인 -> 차감"이 하나의
 * 원자적 연산이 되어 수천 개의 동시 요청이 몰려도 오버셀(초과 판매)이 발생하지 않는다.
 * DB 비관적 락(SELECT ... FOR UPDATE)으로도 동일한 정합성을 보장할 수 있지만, 커넥션을
 * 붙잡고 대기하는 방식이라 대량 동시 요청 시 처리량이 급격히 떨어진다. 반면 이 방식은
 * DB 커넥션을 전혀 점유하지 않고 인메모리 연산 한 번으로 승인 여부를 즉시 결정한다.
 *
 * Redis에서 승인된 이후에는 MySQL에 예매 레코드를 영속화한다({@link ReservationPersistenceService}).
 * Redis가 허용하는 재고(예: 100개) 안에서도, 승인된 요청들이 "동시에" 같은 Event 행을
 * UPDATE하려 들면 InnoDB 데드락이 발생할 수 있다 - 이는 Redis 관문과는 별개의, DB
 * 레이어에서 흔히 나타나는 정상적인 현상이다. 그래서 데드락(PessimisticLockingFailureException)은
 * 즉시 실패 처리하지 않고 짧은 랜덤 백오프 후 자동 재시도한다. 그 외의 영속화 실패
 * (예: 유니크 제약 위반)는 Redis 상태를 되돌리는 보상 트랜잭션을 수행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final int MAX_DEADLOCK_RETRIES = 10;

    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationPersistenceService reservationPersistenceService;
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> reserveStockScript;
    private final DefaultRedisScript<Long> releaseStockScript;

    public ReservationResponse reserve(Long memberId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CustomException(ErrorCode.EVENT_NOT_FOUND));

        if (!event.isReservationOpen(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.RESERVATION_NOT_OPEN);
        }

        long remaining = runReserveScript(event, memberId);

        if (remaining == -3) {
            // Redis 캐시가 비어있는 상태(콜드 스타트/장애 복구) -> DB 값으로 재적재 후 1회 재시도.
            redisTemplate.opsForValue().set(event.stockRedisKey(), String.valueOf(event.getRemainingStock()));
            remaining = runReserveScript(event, memberId);
        }

        if (remaining == -2) {
            throw new CustomException(ErrorCode.ALREADY_RESERVED);
        }
        if (remaining == -1 || remaining == -3) {
            throw new CustomException(ErrorCode.SOLD_OUT);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Reservation reservation = persistReservationWithRetry(member, event);

        // 잔여수량 표시 컬럼 갱신은 별도 트랜잭션 + best-effort. 예약 자체는 이미
        // 확정됐으므로(Redis가 정합성의 기준), 이 표시값 갱신이 끝내 실패해도 예매를
        // 실패로 되돌리지 않는다 - 다음 성공/취소 시점에 다시 갱신될 여지가 있다.
        decreaseDisplayStockBestEffort(event.getId());

        return ReservationResponse.from(reservation);
    }

    private Reservation persistReservationWithRetry(Member member, Event event) {
        int attempt = 0;
        while (true) {
            try {
                return reservationPersistenceService.persistReservation(member, event);
            } catch (DataIntegrityViolationException e) {
                // DB 유니크 제약(member_id, event_id)에 걸린 경우: Redis에서 승인은 됐지만
                // 영속화에 실패했으므로 재고를 되돌려 정합성을 맞춘다(보상 트랜잭션).
                log.warn("Reservation persist failed (duplicate) for member={} event={}, compensating redis",
                        member.getId(), event.getId());
                compensate(event, member.getId());
                throw new CustomException(ErrorCode.ALREADY_RESERVED);
            } catch (PessimisticLockingFailureException e) {
                // 이론상 INSERT 단독 트랜잭션은 데드락을 잘 일으키지 않지만, 락 대기
                // 타임아웃 등 일시적 현상에 대비해 안전망으로 재시도한다.
                attempt++;
                if (attempt >= MAX_DEADLOCK_RETRIES) {
                    log.error("Reservation persist failed after {} retries for member={} event={}, compensating redis",
                            attempt, member.getId(), event.getId(), e);
                    compensate(event, member.getId());
                    throw new CustomException(ErrorCode.RESERVATION_FAILED);
                }
                log.warn("Deadlock/lock-wait on reservation persist (member={}, event={}), retry {}/{}",
                        member.getId(), event.getId(), attempt, MAX_DEADLOCK_RETRIES);
                sleepBackoff(attempt);
            } catch (RuntimeException e) {
                log.error("Unexpected error while persisting reservation, compensating redis", e);
                compensate(event, member.getId());
                throw new CustomException(ErrorCode.RESERVATION_FAILED);
            }
        }
    }

    private void decreaseDisplayStockBestEffort(Long eventId) {
        int attempt = 0;
        while (true) {
            try {
                reservationPersistenceService.decreaseDisplayStock(eventId);
                return;
            } catch (PessimisticLockingFailureException e) {
                attempt++;
                if (attempt >= MAX_DEADLOCK_RETRIES) {
                    log.warn("Giving up updating display stock for event={} after {} retries " +
                            "(reservation itself is still confirmed; Redis remains source of truth)",
                            eventId, attempt, e);
                    return;
                }
                sleepBackoff(attempt);
            } catch (RuntimeException e) {
                log.warn("Unexpected error updating display stock for event={} (non-fatal)", eventId, e);
                return;
            }
        }
    }

    private void sleepBackoff(int attempt) {
        int baseMillis = Math.min(20 * attempt, 200);
        int jitter = ThreadLocalRandom.current().nextInt(0, 30);
        try {
            Thread.sleep(baseMillis + jitter);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    @Transactional
    public void cancel(Long memberId, Long reservationId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Reservation reservation = reservationRepository.findByIdAndMember(reservationId, member)
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_NOT_FOUND));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new CustomException(ErrorCode.RESERVATION_ALREADY_CANCELLED);
        }

        reservation.cancel();
        Event event = reservation.getEvent();

        redisTemplate.execute(
                releaseStockScript,
                List.of(event.stockRedisKey(), event.reservedMembersRedisKey()),
                String.valueOf(memberId),
                String.valueOf(event.getTotalStock())
        );

        eventRepository.increaseRemainingStock(event.getId());
    }

    public List<ReservationResponse> getMyReservations(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        return reservationRepository.findByMemberOrderByReservedAtDesc(member).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    private long runReserveScript(Event event, Long memberId) {
        Long result = redisTemplate.execute(
                reserveStockScript,
                List.of(event.stockRedisKey(), event.reservedMembersRedisKey()),
                String.valueOf(memberId)
        );
        return result == null ? -3 : result;
    }

    private void compensate(Event event, Long memberId) {
        redisTemplate.execute(
                releaseStockScript,
                List.of(event.stockRedisKey(), event.reservedMembersRedisKey()),
                String.valueOf(memberId),
                String.valueOf(event.getTotalStock())
        );
    }
}
