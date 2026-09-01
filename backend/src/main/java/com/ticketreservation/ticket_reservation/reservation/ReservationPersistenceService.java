package com.ticketreservation.ticket_reservation.reservation;

import com.ticketreservation.ticket_reservation.domain.event.Event;
import com.ticketreservation.ticket_reservation.domain.event.EventRepository;
import com.ticketreservation.ticket_reservation.domain.member.Member;
import com.ticketreservation.ticket_reservation.domain.reservation.Reservation;
import com.ticketreservation.ticket_reservation.domain.reservation.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예매 영속화를 "예약 INSERT"와 "이벤트 잔여수량 UPDATE" 두 개의 독립된 트랜잭션으로
 * 분리했다. 처음에는 하나의 트랜잭션에서 같이 처리했는데, 실측(ReservationConcurrencyTest)
 * 결과 수백 건의 InnoDB 데드락이 발생했다 - 예약 INSERT(고유 인덱스 갭락 관여)와 같은
 * Event 행에 대한 UPDATE(행 락)를 한 트랜잭션에 묶으면, 서로 다른 순서로 두 자원을
 * 잠그려는 동시 트랜잭션들 사이에 락 대기 사이클이 만들어지기 쉽다. 두 트랜잭션으로
 * 쪼개면 각 트랜잭션이 자원을 하나씩만 다투므로 데드락이 성립할 수 없고(대기만 발생),
 * 실측상으로도 실패율이 크게 줄었다.
 *
 * ReservationService가 이 메서드들을 재시도할 수 있으려면 매 시도가 "새 트랜잭션"이어야
 * 하므로, 같은 클래스 내 self-invocation이 아니라 별도의 프록시 빈을 거치도록 분리했다.
 */
@Service
@RequiredArgsConstructor
public class ReservationPersistenceService {

    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;

    @Transactional
    public Reservation persistReservation(Member member, Event event) {
        Reservation reservation = Reservation.builder()
                .member(member)
                .event(event)
                .build();
        reservationRepository.save(reservation);
        reservationRepository.flush();
        return reservation;
    }

    @Transactional
    public void decreaseDisplayStock(Long eventId) {
        eventRepository.decreaseRemainingStock(eventId);
    }
}
