package com.ticketreservation.ticket_reservation.event;

import com.ticketreservation.ticket_reservation.event.dto.EventCreateRequest;
import com.ticketreservation.ticket_reservation.event.dto.EventResponse;
import com.ticketreservation.ticket_reservation.event.dto.StockResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request));
    }

    @GetMapping
    public ResponseEntity<Page<EventResponse>> listEvents(Pageable pageable) {
        return ResponseEntity.ok(eventService.listEvents(pageable));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getEvent(eventId));
    }

    @GetMapping("/{eventId}/stock")
    public ResponseEntity<StockResponse> getStock(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getRemainingStock(eventId));
    }
}
