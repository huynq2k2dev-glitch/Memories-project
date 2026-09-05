"use client";

import { type FormEvent, useCallback, useEffect, useState } from "react";

import { authenticatedFetch } from "@/lib/auth-session";

type ShareLinkPermission = "VIEW" | "RSVP";
type ShareLinkStatus = "ACTIVE" | "REVOKED" | "EXPIRED";

type ShareLink = {
  id: string;
  permission: ShareLinkPermission;
  guestId: string | null;
  expiresAt: string | null;
  maxUses: number | null;
  useCount: number;
  status: ShareLinkStatus;
  createdBy: string;
  createdAt: string;
  revokedAt: string | null;
};

type ShareGuest = {
  id: string;
  fullName: string;
  guestGroup: string | null;
  maxPartySize: number;
};

type IssuedShareLink = {
  shareLink: ShareLink;
  accessToken: string;
  sharePath: string;
};

type Problem = {
  detail?: string;
};

export default function MemoryShareLinkEditor({
  memoryId,
  visibility,
}: {
  memoryId: string;
  visibility: "PRIVATE" | "UNLISTED" | "PUBLIC" | "PASSWORD_PROTECTED";
}) {
  const [shareLinks, setShareLinks] = useState<ShareLink[]>([]);
  const [guests, setGuests] = useState<ShareGuest[]>([]);
  const [permission, setPermission] = useState<ShareLinkPermission>("VIEW");
  const [guestId, setGuestId] = useState("");
  const [expiresAt, setExpiresAt] = useState("");
  const [maxUses, setMaxUses] = useState("");
  const [issuedUrl, setIssuedUrl] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const [busy, setBusy] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setBusy(true);
    setError("");
    try {
      const [links, eligibleGuests] = await Promise.all([
        requestJson<ShareLink[]>(`/api/memories/${memoryId}/share-links`),
        requestJson<ShareGuest[]>(
          `/api/memories/${memoryId}/share-links/guests`,
        ),
      ]);
      setShareLinks(links);
      setGuests(eligibleGuests);
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }, [memoryId]);

  useEffect(() => {
    let active = true;
    void Promise.all([
      requestJson<ShareLink[]>(`/api/memories/${memoryId}/share-links`),
      requestJson<ShareGuest[]>(
        `/api/memories/${memoryId}/share-links/guests`,
      ),
    ])
      .then(([links, eligibleGuests]) => {
        if (active) {
          setShareLinks(links);
          setGuests(eligibleGuests);
        }
      })
      .catch((reason: unknown) => {
        if (active) {
          setError(errorMessage(reason));
        }
      })
      .finally(() => {
        if (active) {
          setBusy(false);
        }
      });
    return () => {
      active = false;
    };
  }, [memoryId]);

  const shareable = visibility === "PRIVATE" || visibility === "UNLISTED";

  async function issue(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setIssuedUrl(null);
    setCopied(false);
    try {
      const response = await authenticatedFetch(
        `/api/memories/${memoryId}/share-links`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            permission,
            guestId: permission === "RSVP" ? guestId || null : null,
            expiresAt: expiresAt
              ? new Date(expiresAt).toISOString()
              : null,
            maxUses: maxUses ? Number(maxUses) : null,
          }),
        },
      );
      if (!response.ok) {
        throw new Error((await readProblem(response)).detail);
      }
      const result = (await response.json()) as IssuedShareLink;
      setIssuedUrl(`${window.location.origin}${result.sharePath}`);
      setShareLinks((current) => [result.shareLink, ...current]);
      setExpiresAt("");
      setMaxUses("");
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  async function revoke(shareLink: ShareLink) {
    if (!window.confirm("Thu hồi link chia sẻ này?")) {
      return;
    }
    setBusy(true);
    setError("");
    try {
      const response = await authenticatedFetch(
        `/api/memories/${memoryId}/share-links/${shareLink.id}/revoke`,
        { method: "POST" },
      );
      if (!response.ok) {
        throw new Error((await readProblem(response)).detail);
      }
      setIssuedUrl(null);
      await load();
    } catch (reason) {
      setError(errorMessage(reason));
      setBusy(false);
    }
  }

  async function copyIssuedUrl() {
    if (!issuedUrl) {
      return;
    }
    await navigator.clipboard.writeText(issuedUrl);
    setCopied(true);
  }

  return (
    <section className="share-link-editor">
      <header>
        <div>
          <h3>Link chia sẻ</h3>
          <p className="form-note">
            Tạo đường dẫn có thời hạn hoặc giới hạn lượt mở. Hãy sao chép ngay sau khi tạo vì đường dẫn đầy đủ chỉ hiển thị một lần.
          </p>
        </div>
        <button type="button" disabled={busy} onClick={() => void load()}>
          Tải lại
        </button>
      </header>

      {!shareable ? (
        <p className="form-note">
          Đường dẫn có kiểm soát dành cho trang riêng tư hoặc trang chỉ dành cho người có đường dẫn. Bạn vẫn có thể thu hồi các đường dẫn cũ.
        </p>
      ) : null}

      <form className="share-link-form" onSubmit={issue}>
        <label>
          Quyền
          <select
            value={permission}
            onChange={(event) => {
              const nextPermission = event.target.value as ShareLinkPermission;
              setPermission(nextPermission);
              if (nextPermission === "VIEW") {
                setGuestId("");
              }
            }}
            disabled={busy || !shareable}
          >
            <option value="VIEW">Chỉ xem kỷ niệm</option>
            <option value="RSVP">Xem và xác nhận tham dự</option>
          </select>
        </label>

        {permission === "RSVP" ? (
          <label>
            Khách mời
            <select
              value={guestId}
              onChange={(event) => setGuestId(event.target.value)}
              disabled={busy || !shareable}
              required
            >
              <option value="">Chọn khách mời đang hoạt động</option>
              {guests.map((guest) => (
                <option key={guest.id} value={guest.id}>
                  {guest.fullName}
                  {guest.guestGroup ? ` — ${guest.guestGroup}` : ""}
                </option>
              ))}
            </select>
          </label>
        ) : null}

        <label>
          Hết hạn (không bắt buộc)
          <input
            type="datetime-local"
            value={expiresAt}
            onChange={(event) => setExpiresAt(event.target.value)}
            disabled={busy || !shareable}
          />
        </label>

        <label>
          Lượt dùng tối đa (không bắt buộc)
          <input
            type="number"
            min={1}
            value={maxUses}
            onChange={(event) => setMaxUses(event.target.value)}
            disabled={busy || !shareable}
          />
        </label>

        <button type="submit" disabled={busy || !shareable}>
          {busy ? "Đang xử lý…" : "Phát hành link"}
        </button>
      </form>

      {issuedUrl ? (
        <div className="issued-share-link" aria-live="polite">
          <p>Sao chép và lưu đường dẫn trước khi rời trang.</p>
          <input value={issuedUrl} readOnly aria-label="Link chia sẻ mới" />
          <button type="button" onClick={() => void copyIssuedUrl()}>
            {copied ? "Đã sao chép" : "Sao chép link"}
          </button>
        </div>
      ) : null}

      {error ? (
        <p className="form-note form-error" role="alert">
          {error}
        </p>
      ) : null}

      <div className="share-link-list">
        {shareLinks.length === 0 && !busy ? (
          <p className="form-note">Chưa có link chia sẻ.</p>
        ) : null}
        {shareLinks.map((shareLink) => {
          const exhausted =
            shareLink.maxUses !== null &&
            shareLink.useCount >= shareLink.maxUses;
          return (
            <article key={shareLink.id} className="share-link-card">
              <header>
                <div>
                  <strong>{shareLink.permission}</strong>
                  <span>
                    {shareLink.useCount}/
                    {shareLink.maxUses ?? "không giới hạn"} lượt
                    {exhausted ? " · đã hết lượt" : ""}
                  </span>
                </div>
                <span className="status-badge">{shareLink.status}</span>
              </header>
              <p>
                Hết hạn: {shareLink.expiresAt ? formatTime(shareLink.expiresAt) : "Không"}
              </p>
              {shareLink.guestId ? (
                <p>
                  Guest: {guestName(guests, shareLink.guestId)}
                </p>
              ) : null}
              <small>Tạo lúc {formatTime(shareLink.createdAt)}</small>
              {shareLink.status === "ACTIVE" ? (
                <button
                  className="danger-button"
                  type="button"
                  disabled={busy}
                  onClick={() => void revoke(shareLink)}
                >
                  Thu hồi
                </button>
              ) : null}
            </article>
          );
        })}
      </div>
    </section>
  );
}

async function requestJson<T>(path: string): Promise<T> {
  const response = await authenticatedFetch(path, { cache: "no-store" });
  if (!response.ok) {
    throw new Error((await readProblem(response)).detail);
  }
  return (await response.json()) as T;
}

async function readProblem(response: Response) {
  try {
    const problem = (await response.json()) as Problem;
    return { detail: problem.detail ?? "Không thể xử lý link chia sẻ." };
  } catch {
    return { detail: "Không thể xử lý link chia sẻ." };
  }
}

function guestName(guests: ShareGuest[], guestId: string) {
  return guests.find((guest) => guest.id === guestId)?.fullName ?? guestId;
}

function formatTime(value: string) {
  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function errorMessage(reason: unknown) {
  return reason instanceof Error
    ? reason.message
    : "Dịch vụ tạm thời không khả dụng.";
}
