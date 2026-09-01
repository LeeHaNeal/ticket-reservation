package com.ticketreservation.ticket_reservation.event.dto;

public record StockResponse(Long eventId, int remainingStock, String source) {
}
