import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from "react";
import { clearAuth, loadAuth, saveAuth, type StoredAuth } from "@/lib/api/token";

type AuthContextValue = {
  auth: StoredAuth | null;
  isAuthenticated: boolean;
  setAuth: (next: StoredAuth) => void;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

type AuthProviderProps = { children: ReactNode };

export function AuthProvider({ children }: AuthProviderProps) {
  const [auth, setAuthState] = useState<StoredAuth | null>(() => loadAuth());

  const setAuth = useCallback((next: StoredAuth) => {
    saveAuth(next);
    setAuthState(next);
  }, []);

  const logout = useCallback(() => {
    clearAuth();
    setAuthState(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ auth, isAuthenticated: auth !== null, setAuth, logout }),
    [auth, setAuth, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return ctx;
}
