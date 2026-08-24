"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";

type PublicGuestMessage = {
  id: string;
  guestName: string;
  content: string;
  createdAt: string;
};

export default function GuestMessageSection({
  slug,
  messages,
  canSubmit,
}: {
  slug: string;
  messages: PublicGuestMessage[];
  canSubmit: boolean;
}) {
  const router = useRouter();
  const [guestName, setGuestName] = useState("");
  const [content, setContent] = useState("");
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setNotice("");
    setError("");
    try {
      const response = await fetch(
        `/api/v1/public/memories/${encodeURIComponent(slug)}/messages`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ guestName, content }),
        },
      );
      if (!response.ok) {
        throw new Error(await problemDetail(response));
      }
      setContent("");
      setNotice(
        "Đã nhận lời chúc. Nội dung sẽ xuất hiện sau khi đáp ứng chính sách kiểm duyệt của memory.",
      );
      router.refresh();
    } catch (reason) {
      setError(
        reason instanceof Error
          ? reason.message
          : "Không thể gửi lời chúc lúc này.",
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <section
      className={`public-guest-messages${canSubmit ? "" : " public-guest-messages-read-only"}`}
      aria-label="Lời chúc"
    >
      <div className="public-guest-message-list">
        <h2>Lời chúc</h2>
        {messages.length === 0 ? <p>Chưa có lời chúc được hiển thị.</p> : null}
        {messages.map((message) => (
          <article key={message.id}>
            <h3>{message.guestName}</h3>
            <p>{message.content}</p>
            <time dateTime={message.createdAt}>
              {new Intl.DateTimeFormat("vi-VN", {
                dateStyle: "medium",
                timeStyle: "short",
              }).format(new Date(message.createdAt))}
            </time>
          </article>
        ))}
      </div>

      {canSubmit ? (
        <form className="public-guest-message-form" onSubmit={submit}>
          <h2>Gửi lời chúc</h2>
          <label>
            Tên hiển thị
            <input
              value={guestName}
              onChange={(event) => setGuestName(event.target.value)}
              maxLength={200}
              disabled={busy}
              required
            />
          </label>
          <label>
            Nội dung
            <textarea
              value={content}
              onChange={(event) => setContent(event.target.value)}
              maxLength={2000}
              rows={5}
              disabled={busy}
              required
            />
          </label>
          <button type="submit" disabled={busy}>
            {busy ? "Đang gửi…" : "Gửi lời chúc"}
          </button>
          {notice ? (
            <p className="guest-message-success" role="status">
              {notice}
            </p>
          ) : null}
          {error ? (
            <p className="guest-message-error" role="alert">
              {error}
            </p>
          ) : null}
        </form>
      ) : null}
    </section>
  );
}

async function problemDetail(response: Response) {
  try {
    const problem = (await response.json()) as {
      code?: string;
      detail?: string;
    };
    if (problem.code === "GUEST_MESSAGE_RATE_LIMITED") {
      return "Bạn đã gửi quá nhiều lời chúc. Vui lòng thử lại sau.";
    }
    return problem.detail ?? "Không thể gửi lời chúc lúc này.";
  } catch {
    return "Không thể gửi lời chúc lúc này.";
  }
}
