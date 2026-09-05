"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { type FormEvent, useEffect, useRef, useState } from "react";

type VerificationState = "verifying" | "verified" | "expired" | "invalid" | "unavailable";

const pendingVerifications = new Map<string, Promise<VerificationState>>();

export default function VerifyEmailClient() {
  const router = useRouter();
  const tokenRef = useRef<string | null | undefined>(undefined);
  const [state, setState] = useState<VerificationState>("verifying");
  const [email, setEmail] = useState("");
  const [resendAccepted, setResendAccepted] = useState(false);
  const [resending, setResending] = useState(false);

  useEffect(() => {
    if (tokenRef.current === undefined) {
      tokenRef.current = new URLSearchParams(window.location.hash.slice(1)).get("token");
      window.history.replaceState(null, "", "/verify-email");
    }

    const token = tokenRef.current;
    if (!token) {
      setState("invalid");
      return;
    }

    let active = true;
    void verifyOnce(token).then((result) => {
      if (active) {
        setState(result);
        if (result === "verified") {
          router.replace("/login?verified=1");
        }
      }
    });
    return () => {
      active = false;
    };
  }, [router]);

  async function resend(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setResending(true);
    setResendAccepted(false);
    try {
      const response = await fetch("/api/auth/email-verifications/resend", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email }),
      });
      setResendAccepted(response.status === 202);
    } catch {
      setResendAccepted(false);
    } finally {
      setResending(false);
    }
  }

  const content = verificationContent(state);

  return (
    <main>
      <section className="verification-card" aria-live="polite">
        <p className="eyebrow">Xác thực tài khoản</p>
        <h1>{content.title}</h1>
        <p className="summary">{content.description}</p>

        {state === "verified" ? (
          <Link className="primary-link" href="/login">
            Tiếp tục đăng nhập
          </Link>
        ) : null}

        {state === "expired" || state === "invalid" ? (
          <form className="resend-form" onSubmit={resend}>
            <label htmlFor="verification-email">Email</label>
            <input
              id="verification-email"
              name="email"
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
            <button type="submit" disabled={resending}>
              {resending ? "Đang gửi…" : "Gửi lại liên kết"}
            </button>
            {resendAccepted ? (
              <p className="form-note">
                Nếu tài khoản đang chờ xác thực, một liên kết mới đã được gửi.
              </p>
            ) : null}
          </form>
        ) : null}
      </section>
    </main>
  );
}

async function verifyOnce(token: string): Promise<VerificationState> {
  const existingRequest = pendingVerifications.get(token);
  if (existingRequest) {
    return existingRequest;
  }

  const request = confirmToken(token).finally(() => {
    pendingVerifications.delete(token);
  });
  pendingVerifications.set(token, request);
  return request;
}

async function confirmToken(token: string): Promise<VerificationState> {
  try {
    const response = await fetch("/api/auth/email-verifications/confirm", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ token }),
      cache: "no-store",
    });
    if (response.ok) {
      return "verified";
    }

    const problem = (await response.json()) as { code?: string };
    if (problem.code === "VERIFICATION_TOKEN_EXPIRED") {
      return "expired";
    }
    if (problem.code === "VERIFICATION_TOKEN_INVALID") {
      return "invalid";
    }
    return "unavailable";
  } catch {
    return "unavailable";
  }
}

function verificationContent(state: VerificationState) {
  switch (state) {
    case "verified":
      return {
        title: "Email đã được xác thực",
        description: "Tài khoản của bạn đã sẵn sàng để đăng nhập.",
      };
    case "expired":
      return {
        title: "Liên kết đã hết hạn",
        description: "Yêu cầu một liên kết mới để tiếp tục xác thực tài khoản.",
      };
    case "invalid":
      return {
        title: "Liên kết không hợp lệ",
        description: "Liên kết có thể đã được sử dụng hoặc không còn hợp lệ.",
      };
    case "unavailable":
      return {
        title: "Chưa thể xác thực",
        description: "Dịch vụ đang tạm gián đoạn. Vui lòng thử lại sau.",
      };
    default:
      return {
        title: "Đang xác thực email",
        description: "Vui lòng chờ trong giây lát.",
      };
  }
}
