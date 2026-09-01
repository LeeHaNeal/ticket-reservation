package com.ticketreservation.ticket_reservation.event.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record EventCreateRequest(
        @NotBlank String name,
        String description,
        String venue,
        @NotNull @Min(1) Integer totalStock,
        @NotNull LocalDateTime reservationStartAt,
        @NotNull @Future LocalDateTime reservationEndAt
) {
}
