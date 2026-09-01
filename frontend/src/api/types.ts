export type MemberRole = 'USER' | 'ADMIN';

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export interface SignUpRequest {
  email: string;
  password: string;
  name: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface EventResponse {
  id: number;
  name: string;
  description: string | null;
  venue: string | null;
  totalStock: number;
  remainingStock: number;
  reservationStartAt: string;
  reservationEndAt: string;
}

export interface EventCreateRequest {
  name: string;
  description?: string;
  venue?: string;
  totalStock: number;
  reservationStartAt: string;
  reservationEndAt: string;
}

export interface StockResponse {
  eventId: number;
  remainingStock: number;
  source: string;
}

export type ReservationStatus = 'CONFIRMED' | 'CANCELLED';

export interface ReservationResponse {
  reservationId: number;
  eventId: number;
  eventName: string;
  status: ReservationStatus;
  reservedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface FieldError {
  field: string;
  reason: string;
}

export interface ApiErrorResponse {
  code: string;
  message: string;
  timestamp: string;
  fieldErrors?: FieldError[];
}
