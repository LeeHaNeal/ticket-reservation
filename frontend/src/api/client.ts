import axios from 'axios';
import type { ApiErrorResponse } from './types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081';
export const TOKEN_STORAGE_KEY = 'ticket_reservation_token';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/** axios 에러에서 백엔드가 내려주는 표준 에러 메시지를 최대한 안전하게 뽑아낸다. */
export function extractErrorMessage(error: unknown, fallback = '요청 처리 중 오류가 발생했습니다.'): string {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data?.message ?? fallback;
  }
  return fallback;
}
