import { apiClient } from './client';
import type { LoginRequest, SignUpRequest, TokenResponse } from './types';

export function signup(data: SignUpRequest) {
  return apiClient.post<TokenResponse>('/api/auth/signup', data).then((r) => r.data);
}

export function login(data: LoginRequest) {
  return apiClient.post<TokenResponse>('/api/auth/login', data).then((r) => r.data);
}
