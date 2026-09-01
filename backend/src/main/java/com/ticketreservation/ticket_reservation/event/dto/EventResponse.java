package com.ticketreservation.ticket_reservation.event.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ticketreservation.ticket_reservation.domain.event.Event;

import java.io.Serializable;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventResponse(
        Long id,
        String name,
        String description,
        String venue,
        int totalStock,
        int remainingStock,
        LocalDateTime reservationStartAt,
        LocalDateTime reservationEndAt
) implements Serializable {

    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getVenue(),
                event.getTotalStock(),
                event.getRemainingStock(),
                event.getReservationStartAt(),
                event.getReservationEndAt()
        );
    }
}
