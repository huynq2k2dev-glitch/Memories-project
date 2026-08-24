"use client";

import { useRouter } from "next/navigation";
import { type FormEvent, useState } from "react";

export default function MemoryUnlockForm({ slug }: { slug: string }) {
  const router = useRouter();
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function unlock(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const response = await fetch(
        `/api/v1/public/memories/${encodeURIComponent(slug)}/unlock`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ password }),
        },
      );
      if (!response.ok) {
        setError("Mật khẩu không đúng hoặc memory không còn khả dụng.");
        return;
      }
      setPassword("");
      router.refresh();
    } catch {
      setError("Dịch vụ tạm thời không khả dụng.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="public-memory-shell public-memory-message">
      <section className="memory-access-card">
        <p className="eyebrow">Memory được bảo vệ</p>
        <h1>Nhập mật khẩu để xem</h1>
        <form className="auth-form" onSubmit={unlock}>
          <label htmlFor="memory-access-password">Mật khẩu</label>
          <input
            id="memory-access-password"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="current-password"
            maxLength={72}
            required
          />
          <button type="submit" disabled={busy}>
            {busy ? "Đang xác nhận…" : "Mở memory"}
          </button>
          {error ? (
            <p className="form-note form-error" role="alert">
              {error}
            </p>
          ) : null}
        </form>
      </section>
    </main>
  );
}
