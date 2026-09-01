import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { TOKEN_STORAGE_KEY } from '../api/client';
import { decodeJwtPayload, isTokenExpired } from '../api/jwt';
import type { MemberRole } from '../api/types';

interface AuthState {
  isAuthenticated: boolean;
  email: string | null;
  role: MemberRole | null;
  login: (token: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthState | undefined>(undefined);

function readInitialAuth() {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY);
  if (!token) return { email: null, role: null };

  const payload = decodeJwtPayload(token);
  if (!payload || isTokenExpired(payload)) {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    return { email: null, role: null };
  }
  return { email: payload.sub, role: payload.role };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [{ email, role }, setAuth] = useState(readInitialAuth);

  useEffect(() => {
    // 다른 탭에서 로그인/로그아웃했을 때도 동기화되도록.
    function handleStorage(e: StorageEvent) {
      if (e.key === TOKEN_STORAGE_KEY) {
        setAuth(readInitialAuth());
      }
    }
    window.addEventListener('storage', handleStorage);
    return () => window.removeEventListener('storage', handleStorage);
  }, []);

  const value = useMemo<AuthState>(
    () => ({
      isAuthenticated: email !== null,
      email,
      role,
      login: (token: string) => {
        localStorage.setItem(TOKEN_STORAGE_KEY, token);
        setAuth(readInitialAuth());
      },
      logout: () => {
        localStorage.removeItem(TOKEN_STORAGE_KEY);
        setAuth({ email: null, role: null });
      },
    }),
    [email, role]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}
