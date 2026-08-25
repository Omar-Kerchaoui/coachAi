import { createContext, useCallback, useContext, useMemo, useRef, useState, type ReactNode } from "react";
import { api } from "../lib/api";

interface AuthUser {
  id: string;
  email: string;
}

interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

// Tokens are kept in memory only (never localStorage) per docs/SECURITY.md §9 —
// a page reload requires re-authentication until a refresh-cookie flow lands.
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const tokensRef = useRef<AuthTokens | null>(null);

  useMemo(() => {
    const interceptorId = api.interceptors.request.use((config) => {
      if (tokensRef.current) {
        config.headers.Authorization = `Bearer ${tokensRef.current.accessToken}`;
      }
      return config;
    });
    return interceptorId;
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const { data } = await api.post<{ accessToken: string; refreshToken: string; user: AuthUser }>(
      "/auth/login",
      { email, password }
    );
    tokensRef.current = { accessToken: data.accessToken, refreshToken: data.refreshToken };
    setUser(data.user);
  }, []);

  const register = useCallback(async (email: string, password: string) => {
    await api.post("/auth/register", { email, password });
  }, []);

  const logout = useCallback(async () => {
    try {
      await api.post("/auth/logout");
    } finally {
      tokensRef.current = null;
      setUser(null);
    }
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ user, isAuthenticated: user !== null, login, register, logout }),
    [user, login, register, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return ctx;
}
