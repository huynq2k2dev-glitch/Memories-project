"use client";

import Link from "next/link";
import { type FormEvent, useState } from "react";

type RegistrationState =
  | "idle"
  | "submitting"
  | "registered"
  | "duplicate"
  | "invalid"
  | "unavailable";

export default function RegisterClient() {
  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [state, setState] = useState<RegistrationState>("idle");
  const [clientError, setClientError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setClientError("");
    if (password !== confirmPassword) {
      setClientError("Mật khẩu xác nhận chưa khớp.");
      return;
    }
    setState("submitting");
    try {
      const response = await fetch("/api/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ displayName, email, password }),
      });
      if (response.ok) {
        setPassword("");
        setConfirmPassword("");
        setState("registered");
        return;
      }
      const problem = (await response.json()) as { code?: string };
      setState(
        problem.code === "EMAIL_ALREADY_REGISTERED"
          ? "duplicate"
          : problem.code === "VALIDATION_FAILED"
            ? "invalid"
            : "unavailable",
      );
    } catch {
      setState("unavailable");
    }
  }

  if (state === "registered") {
    return (
      <main className="page-shell centered-page">
        <section className="auth-card" aria-live="polite">
          <p className="eyebrow">Đăng ký thành công</p>
          <h1>Kiểm tra email của bạn</h1>
          <p className="summary">
            Chúng tôi đã gửi liên kết xác thực tới <strong>{email}</strong>. Mở liên
            kết đó để kích hoạt tài khoản trước khi đăng nhập.
          </p>
          <Link className="secondary-link" href="/login">
            Về trang đăng nhập
          </Link>
        </section>
      </main>
    );
  }

  const message = clientError || registrationMessage(state);
  return (
    <main className="page-shell centered-page">
      <section className="auth-card" aria-labelledby="register-title">
        <p className="eyebrow">Tạo tài khoản</p>
        <h1 id="register-title">Bắt đầu lưu giữ câu chuyện</h1>
        <p className="summary">Chỉ mất một phút để tạo không gian kỷ niệm đầu tiên.</p>
        <form className="auth-form" onSubmit={submit}>
          <label htmlFor="register-name">Tên hiển thị</label>
          <input
            id="register-name"
            autoComplete="name"
            maxLength={120}
            required
            value={displayName}
            onChange={(event) => setDisplayName(event.target.value)}
          />
          <label htmlFor="register-email">Email</label>
          <input
            id="register-email"
            type="email"
            autoComplete="email"
            maxLength={320}
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
          <label htmlFor="register-password">Mật khẩu</label>
          <input
            id="register-password"
            type="password"
            autoComplete="new-password"
            minLength={12}
            maxLength={72}
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
          <p className="field-hint">Từ 12 đến 72 ký tự.</p>
          <label htmlFor="register-confirm-password">Xác nhận mật khẩu</label>
          <input
            id="register-confirm-password"
            type="password"
            autoComplete="new-password"
            minLength={12}
            maxLength={72}
            required
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
          />
          <button type="submit" disabled={state === "submitting"}>
            {state === "submitting" ? "Đang tạo tài khoản…" : "Đăng ký"}
          </button>
          {message ? (
            <p className="form-note form-error" role="alert">
              {message}
            </p>
          ) : null}
        </form>
        <p className="auth-switch">
          Đã có tài khoản? <Link href="/login">Đăng nhập</Link>
        </p>
      </section>
    </main>
  );
}

function registrationMessage(state: RegistrationState) {
  switch (state) {
    case "duplicate":
      return "Email này đã được đăng ký. Hãy dùng email khác hoặc đăng nhập.";
    case "invalid":
      return "Thông tin chưa hợp lệ. Kiểm tra email và mật khẩu từ 12 đến 72 ký tự.";
    case "unavailable":
      return "Chưa thể tạo tài khoản. Vui lòng thử lại sau.";
    default:
      return "";
  }
}
