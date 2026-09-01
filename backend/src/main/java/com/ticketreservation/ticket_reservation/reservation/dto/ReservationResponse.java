package com.ticketreservation.ticket_reservation.reservation.dto;

import com.ticketreservation.ticket_reservation.domain.reservation.Reservation;
import com.ticketreservation.ticket_reservation.domain.reservation.ReservationStatus;

import java.time.LocalDateTime;

public record ReservationResponse(
        Long reservationId,
        Long eventId,
        String eventName,
        ReservationStatus status,
        LocalDateTime reservedAt
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getEvent().getId(),
                reservation.getEvent().getName(),
                reservation.getStatus(),
                reservation.getReservedAt()
        );
    }
}
