"use client";

import { useCallback, useEffect, useState } from "react";

import { authenticatedFetch } from "@/lib/auth-session";

type GuestMessageStatus = "PENDING" | "APPROVED" | "REJECTED" | "HIDDEN";

type GuestMessage = {
  id: string;
  guestName: string;
  content: string;
  status: GuestMessageStatus;
  moderatedBy: string | null;
  moderatedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

type GuestMessagePage = {
  items: GuestMessage[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};

const STATUS_OPTIONS: Array<GuestMessageStatus | ""> = [
  "",
  "PENDING",
  "APPROVED",
  "REJECTED",
  "HIDDEN",
];

export default function MemoryMessageEditor({
  memoryId,
  memoryStatus,
  memoryVersion,
  moderationEnabled,
  onMemoryChanged,
}: {
  memoryId: string;
  memoryStatus: "DRAFT" | "PUBLISHED" | "ARCHIVED";
  memoryVersion: number;
  moderationEnabled: boolean;
  onMemoryChanged: () => Promise<void>;
}) {
  const [status, setStatus] = useState<GuestMessageStatus | "">("");
  const [page, setPage] = useState(0);
  const [messages, setMessages] = useState<GuestMessagePage | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const loadMessages = useCallback(async () => {
    setBusy(true);
    setError("");
    try {
      const query = new URLSearchParams({
        page: page.toString(),
        size: "20",
      });
      if (status) {
        query.set("status", status);
      }
      const response = await authenticatedFetch(
        `/api/memories/${memoryId}/messages?${query.toString()}`,
        { cache: "no-store" },
      );
      if (!response.ok) {
        throw new Error(await problemDetail(response));
      }
      setMessages((await response.json()) as GuestMessagePage);
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }, [memoryId, page, status]);

  useEffect(() => {
    void Promise.resolve().then(loadMessages);
  }, [loadMessages]);

  async function moderate(messageId: string, targetStatus: GuestMessageStatus) {
    setBusy(true);
    setError("");
    try {
      const response = await authenticatedFetch(
        `/api/memories/${memoryId}/messages/${messageId}`,
        {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ status: targetStatus }),
        },
      );
      if (!response.ok) {
        throw new Error(await problemDetail(response));
      }
      await loadMessages();
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  async function toggleModeration() {
    setBusy(true);
    setError("");
    try {
      const response = await authenticatedFetch(
        `/api/memories/${memoryId}/messages/settings`,
        {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            enabled: !moderationEnabled,
            version: memoryVersion,
          }),
        },
      );
      if (!response.ok) {
        throw new Error(await problemDetail(response));
      }
      await onMemoryChanged();
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="message-moderation-editor">
      <header>
        <div>
          <h3>Kiểm duyệt lời chúc</h3>
          <p className="form-note">
            Chế độ hiện tại: {moderationEnabled ? "chờ duyệt" : "tự động hiển thị"}.
          </p>
        </div>
        <button
          type="button"
          disabled={busy || memoryStatus === "ARCHIVED"}
          onClick={() => void toggleModeration()}
        >
          {moderationEnabled ? "Tắt kiểm duyệt" : "Bật kiểm duyệt"}
        </button>
      </header>

      <label>
        Lọc trạng thái
        <select
          value={status}
          onChange={(event) => {
            setStatus(event.target.value as GuestMessageStatus | "");
            setPage(0);
          }}
          disabled={busy}
        >
          {STATUS_OPTIONS.map((option) => (
            <option key={option || "ALL"} value={option}>
              {option || "TẤT CẢ"}
            </option>
          ))}
        </select>
      </label>

      {error ? (
        <p className="form-note form-error" role="alert">
          {error}
        </p>
      ) : null}
      {messages?.items.length === 0 ? <p>Không có lời chúc phù hợp.</p> : null}
      <div className="message-moderation-list" aria-busy={busy}>
        {messages?.items.map((message) => (
          <article key={message.id}>
            <header>
              <strong>{message.guestName}</strong>
              <span>{message.status}</span>
            </header>
            <p>{message.content}</p>
            <small>{formatTime(message.createdAt)}</small>
            <div className="message-moderation-actions">
              {transitions(message.status).map((transition) => (
                <button
                  key={transition.status}
                  type="button"
                  disabled={busy}
                  onClick={() => void moderate(message.id, transition.status)}
                >
                  {transition.label}
                </button>
              ))}
            </div>
          </article>
        ))}
      </div>

      {messages && messages.totalPages > 1 ? (
        <nav className="pagination" aria-label="Phân trang lời chúc">
          <button
            type="button"
            disabled={busy || page === 0}
            onClick={() => setPage((current) => current - 1)}
          >
            Trang trước
          </button>
          <span>
            Trang {messages.page + 1}/{messages.totalPages}
          </span>
          <button
            type="button"
            disabled={busy || page + 1 >= messages.totalPages}
            onClick={() => setPage((current) => current + 1)}
          >
            Trang sau
          </button>
        </nav>
      ) : null}
    </section>
  );
}

function transitions(status: GuestMessageStatus) {
  switch (status) {
    case "PENDING":
      return [
        { status: "APPROVED" as const, label: "Duyệt" },
        { status: "REJECTED" as const, label: "Từ chối" },
      ];
    case "APPROVED":
      return [{ status: "HIDDEN" as const, label: "Ẩn" }];
    case "HIDDEN":
      return [{ status: "APPROVED" as const, label: "Hiển thị lại" }];
    case "REJECTED":
      return [{ status: "PENDING" as const, label: "Đưa về chờ duyệt" }];
  }
}

function formatTime(value: string) {
  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

async function problemDetail(response: Response) {
  try {
    const problem = (await response.json()) as { detail?: string };
    return problem.detail ?? "Không thể xử lý lời chúc.";
  } catch {
    return "Không thể xử lý lời chúc.";
  }
}

function errorMessage(reason: unknown) {
  return reason instanceof Error
    ? reason.message
    : "Dịch vụ tạm thời không khả dụng.";
}
