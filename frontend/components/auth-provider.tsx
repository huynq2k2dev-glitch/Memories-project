"use client";

import {
  createContext,
  type ReactNode,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";

import type { CurrentAccount } from "@/lib/api-types";
import {
  authenticatedFetch,
  clearAccessToken,
  logoutCurrentSession,
  restoreAccessToken,
  storeAccessToken,
  subscribeToSessionInvalidation,
} from "@/lib/auth-session";

type AuthStatus = "loading" | "authenticated" | "anonymous";

type AuthContextValue = {
  status: AuthStatus;
  account: CurrentAccount | null;
  signIn: (token: string) => Promise<CurrentAccount>;
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export default function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [account, setAccount] = useState<CurrentAccount | null>(null);

  const becomeAnonymous = useCallback(() => {
    setAccount(null);
    setStatus("anonymous");
  }, []);

  const loadAccount = useCallback(async () => {
    const response = await authenticatedFetch("/api/auth/me", {
      cache: "no-store",
    });
    if (!response.ok) {
      throw new Error("Không thể đọc tài khoản hiện tại.");
    }
    const currentAccount = (await response.json()) as CurrentAccount;
    setAccount(currentAccount);
    setStatus("authenticated");
    return currentAccount;
  }, []);

  useEffect(() => subscribeToSessionInvalidation(becomeAnonymous), [becomeAnonymous]);

  useEffect(() => {
    let active = true;

    async function restoreSession() {
      const restored = await restoreAccessToken();
      if (!active) {
        return;
      }
      if (!restored) {
        becomeAnonymous();
        return;
      }
      try {
        await loadAccount();
      } catch {
        if (active) {
          clearAccessToken();
          becomeAnonymous();
        }
      }
    }

    void restoreSession();
    return () => {
      active = false;
    };
  }, [becomeAnonymous, loadAccount]);

  const signIn = useCallback(
    async (token: string) => {
      storeAccessToken(token);
      try {
        return await loadAccount();
      } catch (error) {
        clearAccessToken();
        becomeAnonymous();
        throw error;
      }
    },
    [becomeAnonymous, loadAccount],
  );

  const signOut = useCallback(async () => {
    await logoutCurrentSession();
    becomeAnonymous();
  }, [becomeAnonymous]);

  const value = useMemo(
    () => ({ status, account, signIn, signOut }),
    [account, signIn, signOut, status],
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
