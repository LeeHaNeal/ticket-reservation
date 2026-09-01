export interface JwtPayload {
  sub: string; // email
  role: 'USER' | 'ADMIN';
  exp: number;
  iat: number;
}

/** 서명 검증 없이 페이로드만 디코딩한다 (UI 표시용 - 실제 인가는 항상 백엔드가 담당). */
export function decodeJwtPayload(token: string): JwtPayload | null {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload) as JwtPayload;
  } catch {
    return null;
  }
}

export function isTokenExpired(payload: JwtPayload): boolean {
  return Date.now() >= payload.exp * 1000;
}
