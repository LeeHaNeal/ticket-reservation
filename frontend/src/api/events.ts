import { apiClient } from './client';
import type { EventCreateRequest, EventResponse, PageResponse, StockResponse } from './types';

export function listEvents(page = 0, size = 12, sort?: string) {
  return apiClient
    .get<PageResponse<EventResponse>>('/api/events', { params: { page, size, sort } })
    .then((r) => r.data);
}

export function getEvent(eventId: number) {
  return apiClient.get<EventResponse>(`/api/events/${eventId}`).then((r) => r.data);
}

export function getStock(eventId: number) {
  return apiClient.get<StockResponse>(`/api/events/${eventId}/stock`).then((r) => r.data);
}

export function createEvent(data: EventCreateRequest) {
  return apiClient.post<EventResponse>('/api/events', data).then((r) => r.data);
}
