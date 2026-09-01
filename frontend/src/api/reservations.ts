import { apiClient } from './client';
import type { ReservationResponse } from './types';

export function reserve(eventId: number) {
  return apiClient.post<ReservationResponse>(`/api/events/${eventId}/reservations`).then((r) => r.data);
}

export function cancelReservation(reservationId: number) {
  return apiClient.delete<void>(`/api/reservations/${reservationId}`);
}

export function getMyReservations() {
  return apiClient.get<ReservationResponse[]>('/api/reservations/me').then((r) => r.data);
}
