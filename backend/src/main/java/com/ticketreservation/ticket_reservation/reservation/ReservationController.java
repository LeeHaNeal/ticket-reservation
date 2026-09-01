package com.ticketreservation.ticket_reservation.reservation;

import com.ticketreservation.ticket_reservation.reservation.dto.ReservationResponse;
import com.ticketreservation.ticket_reservation.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping("/api/events/{eventId}/reservations")
    public ResponseEntity<ReservationResponse> reserve(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                         @PathVariable Long eventId) {
        ReservationResponse response = reservationService.reserve(userDetails.getMemberId(), eventId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/api/reservations/{reservationId}")
    public ResponseEntity<Void> cancel(@AuthenticationPrincipal CustomUserDetails userDetails,
                                        @PathVariable Long reservationId) {
        reservationService.cancel(userDetails.getMemberId(), reservationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/reservations/me")
    public ResponseEntity<List<ReservationResponse>> myReservations(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(reservationService.getMyReservations(userDetails.getMemberId()));
    }
}
