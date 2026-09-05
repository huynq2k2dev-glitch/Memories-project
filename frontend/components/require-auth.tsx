"use client";

import { useRouter } from "next/navigation";
import { type ReactNode, useEffect } from "react";

import { useAuth } from "@/components/auth-provider";

export default function RequireAuth({ children }: { children: ReactNode }) {
  const { status } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (status === "anonymous") {
      const destination = `${window.location.pathname}${window.location.search}`;
      router.replace(`/login?next=${encodeURIComponent(destination)}`);
    }
  }, [router, status]);

  if (status !== "authenticated") {
    return (
      <main className="page-shell auth-loading" aria-live="polite">
        <div className="loading-panel">
          {status === "loading" ? "Đang khôi phục phiên…" : "Đang chuyển tới đăng nhập…"}
        </div>
      </main>
    );
  }

  return children;
}
