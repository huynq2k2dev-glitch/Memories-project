"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { type FormEvent, useEffect, useState } from "react";

import { useAuth } from "@/components/auth-provider";
import { safeInternalPath } from "@/lib/navigation";

type LoginState =
  | "idle"
  | "submitting"
  | "invalid"
  | "unverified"
  | "locked"
  | "limited"
  | "unavailable";

export default function LoginClient({
  returnTo,
  verified,
}: {
  returnTo?: string;
  verified: boolean;
}) {
  const { signIn, status } = useAuth();
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [state, setState] = useState<LoginState>("idle");
  const destination = safeInternalPath(returnTo, "/memories");

  useEffect(() => {
    if (status === "authenticated") {
      router.replace(destination);
    }
  }, [destination, router, status]);

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
        await signIn(payload.accessToken);
        setPassword("");
        router.replace(destination);
        return;
      }

      const problem = (await response.json()) as { code?: string };
      setState(loginErrorState(problem.code));
    } catch {
      setState("unavailable");
    }
  }

  const message = loginMessage(state);
  return (
    <main className="page-shell centered-page">
      <section className="auth-card" aria-labelledby="login-title">
        <p className="eyebrow">Tài khoản</p>
        <h1 id="login-title">Đăng nhập</h1>
        <p className="summary">Tiếp tục tạo và lưu giữ những kỷ niệm của bạn.</p>
        {verified ? (
          <p className="success-note" role="status">
            Email đã được xác thực. Bạn có thể đăng nhập ngay.
          </p>
        ) : null}
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
            minLength={12}
            maxLength={72}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
          <button
            type="submit"
            disabled={state === "submitting" || status === "loading"}
          >
            {state === "submitting" ? "Đang đăng nhập…" : "Đăng nhập"}
          </button>
          {message ? (
            <p className="form-note form-error" role="alert">
              {message}
            </p>
          ) : null}
        </form>
        <p className="auth-switch">
          Chưa có tài khoản? <Link href="/register">Đăng ký</Link>
        </p>
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
    case "LOGIN_RATE_LIMITED":
      return "limited";
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
    case "limited":
      return "Bạn đã thử đăng nhập quá nhiều lần. Vui lòng chờ rồi thử lại.";
    case "unavailable":
      return "Chưa thể đăng nhập. Vui lòng thử lại sau.";
    default:
      return null;
  }
}
