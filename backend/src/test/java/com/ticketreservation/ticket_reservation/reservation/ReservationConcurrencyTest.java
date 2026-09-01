package com.ticketreservation.ticket_reservation.reservation;

import com.ticketreservation.ticket_reservation.common.exception.CustomException;
import com.ticketreservation.ticket_reservation.domain.event.Event;
import com.ticketreservation.ticket_reservation.domain.event.EventRepository;
import com.ticketreservation.ticket_reservation.domain.member.Member;
import com.ticketreservation.ticket_reservation.domain.member.MemberRepository;
import com.ticketreservation.ticket_reservation.domain.member.MemberRole;
import com.ticketreservation.ticket_reservation.domain.reservation.ReservationRepository;
import com.ticketreservation.ticket_reservation.domain.reservation.ReservationStatus;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 선착순 예매의 핵심 요구사항인 "재고를 초과해서 팔리지 않는다(no oversell)"를
 * 실제 동시 요청으로 증명하는 테스트.
 *
 * 재고 100개짜리 이벤트에 서로 다른 회원 1000명이 동시에 예매를 시도했을 때,
 * 정확히 100건만 성공하고 나머지 900건은 SOLD_OUT으로 실패해야 한다.
 *
 * 사전 조건: 로컬에서 docker-compose로 MySQL/Redis가 떠 있어야 한다.
 *   docker compose up -d
 *   ./mvnw test -Dtest=ReservationConcurrencyTest
 */
@Slf4j
@SpringBootTest
class ReservationConcurrencyTest {

    private static final int TOTAL_STOCK = 100;
    private static final int MEMBER_COUNT = 1000;
    private static final int THREAD_POOL_SIZE = 32;

    @Autowired
    private ReservationService reservationService;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private Event event;
    private List<Member> members;

    @BeforeEach
    void setUp() {
        event = eventRepository.save(Event.builder()
                .name("동시성 테스트 콘서트")
                .description("오버셀 방지 검증용 이벤트")
                .venue("테스트홀")
                .totalStock(TOTAL_STOCK)
                .reservationStartAt(LocalDateTime.now().minusMinutes(1))
                .reservationEndAt(LocalDateTime.now().plusHours(1))
                .build());

        redisTemplate.opsForValue().set(event.stockRedisKey(), String.valueOf(event.getTotalStock()));
        redisTemplate.delete(event.reservedMembersRedisKey());

        // 이메일에 실행마다 바뀌는 접미사를 붙여, 이전 실행의 잔여 데이터가 남아있어도
        // 유니크 제약(member.uk_member_email)에 걸리지 않도록 한다 (테스트 재실행 안전성).
        String runSuffix = String.valueOf(System.nanoTime());
        members = new ArrayList<>();
        for (int i = 0; i < MEMBER_COUNT; i++) {
            members.add(memberRepository.save(Member.builder()
                    .email("concurrency-test-" + i + "-" + runSuffix + "@example.com")
                    .password("{noop}test-password")
                    .name("tester-" + i)
                    .role(MemberRole.USER)
                    .build()));
        }
    }

    @AfterEach
    void tearDown() {
        // Redis 카운터/SET 정리
        redisTemplate.delete(event.stockRedisKey());
        redisTemplate.delete(event.reservedMembersRedisKey());

        // MySQL 정리 (FK 순서: reservation -> event / member). @AfterEach 메서드는
        // JUnit이 테스트 인스턴스를 통해 직접 리플렉션 호출하므로 Spring 프록시를 거치지
        // 않는다 - 메서드에 @Transactional을 붙여도 무시된다. TransactionTemplate으로
        // 명시적인 트랜잭션을 열어야 한다.
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            reservationRepository.deleteAllByEvent(event);
            memberRepository.deleteAll(members);
            eventRepository.delete(event);
        });
    }

    @Test
    void 재고보다_많은_동시요청이_와도_정확히_재고수량만큼만_성공한다() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        CountDownLatch readyLatch = new CountDownLatch(MEMBER_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(MEMBER_COUNT);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger soldOutCount = new AtomicInteger();
        AtomicInteger otherFailureCount = new AtomicInteger();

        for (Member member : members) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await(); // 모든 스레드가 동시에 출발하도록 정렬
                    reservationService.reserve(member.getId(), event.getId());
                    successCount.incrementAndGet();
                } catch (CustomException e) {
                    if ("SOLD_OUT".equals(e.getErrorCode().name())) {
                        soldOutCount.incrementAndGet();
                    } else {
                        otherFailureCount.incrementAndGet();
                        log.warn("Unexpected CustomException: {}", e.getErrorCode(), e);
                    }
                } catch (Exception e) {
                    otherFailureCount.incrementAndGet();
                    log.warn("Unexpected exception during concurrent reservation", e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(10, TimeUnit.SECONDS);
        startLatch.countDown(); // 동시 발사
        doneLatch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.printf("성공: %d, 매진 실패: %d, 기타 실패: %d%n",
                successCount.get(), soldOutCount.get(), otherFailureCount.get());

        assertThat(otherFailureCount.get()).isZero();
        assertThat(successCount.get()).isEqualTo(TOTAL_STOCK);
        assertThat(soldOutCount.get()).isEqualTo(MEMBER_COUNT - TOTAL_STOCK);

        long confirmedInDb = reservationRepository.countByEventAndStatus(event, ReservationStatus.CONFIRMED);
        assertThat(confirmedInDb).isEqualTo(TOTAL_STOCK);

        String remainingInRedis = redisTemplate.opsForValue().get(event.stockRedisKey());
        assertThat(Integer.parseInt(remainingInRedis)).isZero();
    }
}
