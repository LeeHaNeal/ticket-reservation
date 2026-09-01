package com.ticketreservation.ticket_reservation.domain.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * 단일 UPDATE 문으로 잔여 수량을 원자적으로 1 감소시킨다 (표시용 컬럼 동기화).
     * MySQL의 단일 row UPDATE는 그 자체로 원자적이므로 별도의 비관적 락이 필요 없다.
     * 실제 판매 가능 여부의 최종 판단은 Redis Lua 스크립트가 이미 수행한 뒤이므로,
     * 여기서 rowsAffected == 0 이어도 예매 자체를 막지는 않는다 (표시값 최소 0 보정용).
     */
    @Modifying
    @Query("UPDATE Event e SET e.remainingStock = e.remainingStock - 1 " +
            "WHERE e.id = :eventId AND e.remainingStock > 0")
    int decreaseRemainingStock(@Param("eventId") Long eventId);

    @Modifying
    @Query("UPDATE Event e SET e.remainingStock = e.remainingStock + 1 " +
            "WHERE e.id = :eventId AND e.remainingStock < e.totalStock")
    int increaseRemainingStock(@Param("eventId") Long eventId);
}
