"use client";

import Link from "next/link";
import { type FormEvent, useState } from "react";

import {
  logoutAllSessions,
  logoutCurrentSession,
  storeAccessToken,
} from "@/lib/auth-session";

type LoginState =
  | "idle"
  | "submitting"
  | "signed-in"
  | "invalid"
  | "unverified"
  | "locked"
  | "unavailable";

export default function LoginClient() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [state, setState] = useState<LoginState>("idle");
  const [loggingOut, setLoggingOut] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setState("submitting");
    try {
      const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });
      if (response.ok) {
        const payload = (await response.json()) as { accessToken: string };
        storeAccessToken(payload.accessToken);
        setPassword("");
        setState("signed-in");
        return;
      }

      const problem = (await response.json()) as { code?: string };
      setState(loginErrorState(problem.code));
    } catch {
      setState("unavailable");
    }
  }

  const message = loginMessage(state);

  async function logout(allDevices: boolean) {
    setLoggingOut(true);
    try {
      if (allDevices) {
        await logoutAllSessions();
      } else {
        await logoutCurrentSession();
      }
      setState("idle");
    } finally {
      setLoggingOut(false);
    }
  }

  return (
    <main>
      <section className="login-card" aria-labelledby="login-title">
        <p className="eyebrow">Tài khoản</p>
        <h1 id="login-title">Đăng nhập</h1>
        <p className="summary">Tiếp tục tạo và lưu giữ những kỷ niệm của bạn.</p>

        {state === "signed-in" ? (
          <div className="auth-result" aria-live="polite">
            <p>Đăng nhập thành công.</p>
            <Link className="primary-link" href="/">
              Về trang chính
            </Link>
            <Link className="secondary-link" href="/admin/templates">
              Quản trị template
            </Link>
            <Link className="secondary-link" href="/templates">
              Duyệt template
            </Link>
            <button
              className="secondary-button"
              type="button"
              disabled={loggingOut}
              onClick={() => void logout(false)}
            >
              Đăng xuất phiên này
            </button>
            <button
              className="secondary-button"
              type="button"
              disabled={loggingOut}
              onClick={() => void logout(true)}
            >
              Đăng xuất mọi thiết bị
            </button>
          </div>
        ) : (
          <form className="auth-form" onSubmit={submit}>
            <label htmlFor="login-email">Email</label>
            <input
              id="login-email"
              name="email"
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />

            <label htmlFor="login-password">Mật khẩu</label>
            <input
              id="login-password"
              name="password"
              type="password"
              autoComplete="current-password"
              required
              maxLength={72}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />

            <button type="submit" disabled={state === "submitting"}>
              {state === "submitting" ? "Đang đăng nhập…" : "Đăng nhập"}
            </button>
            {message ? (
              <p className="form-note form-error" role="alert">
                {message}
              </p>
            ) : null}
          </form>
        )}
      </section>
    </main>
  );
}

function loginErrorState(code: string | undefined): LoginState {
  switch (code) {
    case "INVALID_CREDENTIALS":
      return "invalid";
    case "EMAIL_NOT_VERIFIED":
      return "unverified";
    case "ACCOUNT_LOCKED":
      return "locked";
    default:
      return "unavailable";
  }
}

function loginMessage(state: LoginState) {
  switch (state) {
    case "invalid":
      return "Email hoặc mật khẩu không đúng.";
    case "unverified":
      return "Tài khoản chưa xác thực email. Vui lòng kiểm tra hộp thư của bạn.";
    case "locked":
      return "Tài khoản đang tạm khóa. Vui lòng thử lại sau.";
    case "unavailable":
      return "Chưa thể đăng nhập. Vui lòng thử lại sau.";
    default:
      return null;
  }
}
