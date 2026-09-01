package com.ticketreservation.ticket_reservation.domain.reservation;

import com.ticketreservation.ticket_reservation.domain.event.Event;
import com.ticketreservation.ticket_reservation.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservation", uniqueConstraints = {
        // 회원 1명당 이벤트 1건만 예매 가능 (재예매 방지). 취소 후에도 재예매는 허용하지 않는
        // 단순한 정책으로, 포트폴리오 범위에서는 이 제약이 Redis SADD 검사의 최종 안전망 역할을 한다.
        @UniqueConstraint(name = "uk_reservation_member_event", columnNames = {"member_id", "event_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(nullable = false)
    private LocalDateTime reservedAt;

    private LocalDateTime cancelledAt;

    @Builder
    private Reservation(Member member, Event event) {
        this.member = member;
        this.event = event;
        this.status = ReservationStatus.CONFIRMED;
        this.reservedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (this.status == ReservationStatus.CANCELLED) {
            return;
        }
        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }
}
