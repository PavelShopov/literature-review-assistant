import { createContext, useContext, useMemo, useState } from "react";
import { loginUser, logoutUser, registerUser, type AuthUser } from "../api/client";

type AuthContextValue = {
  user: AuthUser | null;
  token: string | null;
  login: (input: { email: string; password: string }) => Promise<void>;
  register: (input: { name: string; email: string; password: string }) => Promise<void>;
  logout: () => Promise<void>;
};

const AUTH_STORAGE_KEY = "auth:session";

const AuthContext = createContext<AuthContextValue | null>(null);

const readStoredSession = (): { user: AuthUser; token: string } | null => {
  const stored = localStorage.getItem(AUTH_STORAGE_KEY);
  if (!stored) return null;

  try {
    return JSON.parse(stored);
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
};

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState(readStoredSession);

  const value = useMemo<AuthContextValue>(
    () => ({
      user: session?.user ?? null,
      token: session?.token ?? null,
      login: async (input) => {
        const nextSession = await loginUser(input);
        localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(nextSession));
        setSession(nextSession);
      },
      register: async (input) => {
        const nextSession = await registerUser(input);
        localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(nextSession));
        setSession(nextSession);
      },
      logout: async () => {
        if (session?.token) {
          await logoutUser(session.token).catch(() => undefined);
        }
        localStorage.removeItem(AUTH_STORAGE_KEY);
        setSession(null);
      },
    }),
    [session],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return context;
}
