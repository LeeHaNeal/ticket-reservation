package com.ticketreservation.ticket_reservation.domain.event;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(length = 200)
    private String venue;

    /** 총 티켓 수량 (고정값, 변경되지 않음) */
    @Column(nullable = false)
    private int totalStock;

    /**
     * 표시용 잔여 수량. 동시성 제어의 실제 기준(source of truth)은 Redis
     * ("event:{id}:stock")이며, 이 컬럼은 예매/취소 시 원자적 UPDATE 문으로
     * 함께 갱신되는 조회/통계용 값이다.
     */
    @Column(nullable = false)
    private int remainingStock;

    @Column(nullable = false)
    private LocalDateTime reservationStartAt;

    @Column(nullable = false)
    private LocalDateTime reservationEndAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Event(String name, String description, String venue, int totalStock,
                   LocalDateTime reservationStartAt, LocalDateTime reservationEndAt) {
        this.name = name;
        this.description = description;
        this.venue = venue;
        this.totalStock = totalStock;
        this.remainingStock = totalStock;
        this.reservationStartAt = reservationStartAt;
        this.reservationEndAt = reservationEndAt;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isReservationOpen(LocalDateTime now) {
        return !now.isBefore(reservationStartAt) && !now.isAfter(reservationEndAt);
    }

    public String stockRedisKey() {
        return "event:" + id + ":stock";
    }

    public String reservedMembersRedisKey() {
        return "event:" + id + ":members";
    }

    public String cacheKey() {
        return "event:" + id + ":info";
    }
}
