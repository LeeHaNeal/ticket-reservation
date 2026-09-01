package com.ticketreservation.ticket_reservation.domain.reservation;

import com.ticketreservation.ticket_reservation.domain.event.Event;
import com.ticketreservation.ticket_reservation.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByMemberAndEvent(Member member, Event event);

    List<Reservation> findByMemberOrderByReservedAtDesc(Member member);

    Optional<Reservation> findByIdAndMember(Long id, Member member);

    long countByEventAndStatus(Event event, ReservationStatus status);

    void deleteAllByEvent(Event event);
}
