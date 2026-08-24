"use client";

import { type FormEvent, useCallback, useEffect, useState } from "react";

import { authenticatedFetch } from "@/lib/auth-session";

type MemoryGuest = {
  id: string;
  fullName: string;
  email: string | null;
  phone: string | null;
  guestGroup: string | null;
  maxPartySize: number;
  note: string | null;
  status: "ACTIVE" | "DISABLED";
  tokenIssued: boolean;
  createdAt: string;
  updatedAt: string;
  version: number;
};

type MemoryGuestPage = {
  items: MemoryGuest[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};

type GuestAccessToken = {
  guestId: string;
  accessToken: string;
  invitationPath: string;
  issuedAt: string;
  version: number;
};

type IssuedInvitation = {
  guestId: string;
  url: string;
};

type Problem = {
  code?: string;
  detail?: string;
};

export default function MemoryGuestEditor({ memoryId }: { memoryId: string }) {
  const [guestPage, setGuestPage] = useState<MemoryGuestPage | null>(null);
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [guestGroup, setGuestGroup] = useState("");
  const [maxPartySize, setMaxPartySize] = useState(1);
  const [note, setNote] = useState("");
  const [issuedInvitation, setIssuedInvitation] =
    useState<IssuedInvitation | null>(null);
  const [busy, setBusy] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(
    async (page = 0) => {
      setBusy(true);
      setError("");
      try {
        setGuestPage(await requestGuests(memoryId, page));
      } catch (reason) {
        setError(errorMessage(reason));
      } finally {
        setBusy(false);
      }
    },
    [memoryId],
  );

  useEffect(() => {
    let active = true;
    void requestGuests(memoryId, 0)
      .then((result) => {
        if (active) {
          setGuestPage(result);
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

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const response = await authenticatedFetch(
        `/api/memories/${memoryId}/guests`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            fullName,
            email: optional(email),
            phone: optional(phone),
            guestGroup: optional(guestGroup),
            maxPartySize,
            note: optional(note),
          }),
        },
      );
      if (!response.ok) {
        throw new Error((await readProblem(response)).detail);
      }
      setFullName("");
      setEmail("");
      setPhone("");
      setGuestGroup("");
      setMaxPartySize(1);
      setNote("");
      setIssuedInvitation(null);
      await load(0);
    } catch (reason) {
      setError(errorMessage(reason));
      setBusy(false);
    }
  }

  function replaceGuest(updated: MemoryGuest) {
    setGuestPage((current) =>
      current
        ? {
            ...current,
            items: current.items.map((guest) =>
              guest.id === updated.id ? updated : guest,
            ),
          }
        : current,
    );
  }

  return (
    <section className="guest-editor">
      <header>
        <div>
          <h3>Khách mời</h3>
          <p className="form-note">
            Guest mới chưa có link. Token cũ bị thu hồi ngay khi phát hành lại;
            guest bị vô hiệu hóa không thể kích hoạt lại trong ticket này.
          </p>
        </div>
        <button
          type="button"
          disabled={busy}
          onClick={() => void load(guestPage?.page ?? 0)}
        >
          Tải lại
        </button>
      </header>

      <form className="guest-form" onSubmit={create}>
        <label htmlFor={`guest-name-${memoryId}`}>Tên khách/nhóm khách</label>
        <input
          id={`guest-name-${memoryId}`}
          value={fullName}
          onChange={(event) => setFullName(event.target.value)}
          maxLength={200}
          required
        />
        <label htmlFor={`guest-email-${memoryId}`}>Email</label>
        <input
          id={`guest-email-${memoryId}`}
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          maxLength={255}
        />
        <label htmlFor={`guest-phone-${memoryId}`}>Điện thoại E.164</label>
        <input
          id={`guest-phone-${memoryId}`}
          value={phone}
          onChange={(event) => setPhone(event.target.value)}
          maxLength={16}
          placeholder="+84901234567"
          pattern="\+[1-9][0-9]{1,14}"
        />
        <label htmlFor={`guest-group-${memoryId}`}>Nhóm khách</label>
        <input
          id={`guest-group-${memoryId}`}
          value={guestGroup}
          onChange={(event) => setGuestGroup(event.target.value)}
          maxLength={100}
        />
        <label htmlFor={`guest-party-${memoryId}`}>Party size tối đa</label>
        <input
          id={`guest-party-${memoryId}`}
          type="number"
          value={maxPartySize}
          onChange={(event) => setMaxPartySize(Number(event.target.value))}
          min={1}
          max={50}
          required
        />
        <label htmlFor={`guest-note-${memoryId}`}>Ghi chú nội bộ</label>
        <textarea
          id={`guest-note-${memoryId}`}
          value={note}
          onChange={(event) => setNote(event.target.value)}
          maxLength={1000}
          rows={3}
        />
        <button type="submit" disabled={busy}>
          {busy ? "Đang xử lý…" : "Thêm khách"}
        </button>
      </form>

      {error ? (
        <p className="form-note form-error" role="alert">
          {error}
        </p>
      ) : null}

      <div className="guest-list">
        {guestPage?.items.length === 0 && !busy ? (
          <p className="form-note">Chưa có khách mời.</p>
        ) : null}
        {guestPage?.items.map((guest) => (
          <GuestCard
            key={`${guest.id}-${guest.version}`}
            memoryId={memoryId}
            guest={guest}
            issuedInvitation={
              issuedInvitation?.guestId === guest.id ? issuedInvitation : null
            }
            onUpdated={replaceGuest}
            onIssued={(result) => {
              replaceGuest({
                ...guest,
                tokenIssued: true,
                updatedAt: result.issuedAt,
                version: result.version,
              });
              setIssuedInvitation({
                guestId: guest.id,
                url: `${window.location.origin}${result.invitationPath}`,
              });
            }}
            onDisabled={async () => {
              if (issuedInvitation?.guestId === guest.id) {
                setIssuedInvitation(null);
              }
              await load(guestPage?.page ?? 0);
            }}
          />
        ))}
      </div>

      {guestPage && guestPage.totalPages > 1 ? (
        <nav className="pagination" aria-label="Phân trang khách mời">
          <button
            type="button"
            disabled={busy || guestPage.page === 0}
            onClick={() => void load(guestPage.page - 1)}
          >
            Trang trước
          </button>
          <span>
            Trang {guestPage.page + 1}/{guestPage.totalPages} · {guestPage.totalItems} khách
          </span>
          <button
            type="button"
            disabled={busy || guestPage.page + 1 >= guestPage.totalPages}
            onClick={() => void load(guestPage.page + 1)}
          >
            Trang sau
          </button>
        </nav>
      ) : null}
    </section>
  );
}

function GuestCard({
  memoryId,
  guest,
  issuedInvitation,
  onUpdated,
  onIssued,
  onDisabled,
}: {
  memoryId: string;
  guest: MemoryGuest;
  issuedInvitation: IssuedInvitation | null;
  onUpdated: (guest: MemoryGuest) => void;
  onIssued: (result: GuestAccessToken) => void;
  onDisabled: () => Promise<void>;
}) {
  const [fullName, setFullName] = useState(guest.fullName);
  const [email, setEmail] = useState(guest.email ?? "");
  const [phone, setPhone] = useState(guest.phone ?? "");
  const [guestGroup, setGuestGroup] = useState(guest.guestGroup ?? "");
  const [maxPartySize, setMaxPartySize] = useState(guest.maxPartySize);
  const [note, setNote] = useState(guest.note ?? "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [copied, setCopied] = useState(false);

  async function update(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const response = await authenticatedFetch(
        `/api/memories/${memoryId}/guests/${guest.id}`,
        {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            fullName,
            email: optional(email),
            phone: optional(phone),
            guestGroup: optional(guestGroup),
            maxPartySize,
            note: optional(note),
            version: guest.version,
          }),
        },
      );
      if (!response.ok) {
        throw new Error((await readProblem(response)).detail);
      }
      onUpdated((await response.json()) as MemoryGuest);
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  async function issue() {
    setBusy(true);
    setError("");
    setCopied(false);
    try {
      const response = await authenticatedFetch(
        `/api/memories/${memoryId}/guests/${guest.id}/access-token`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ version: guest.version }),
        },
      );
      if (!response.ok) {
        throw new Error((await readProblem(response)).detail);
      }
      onIssued((await response.json()) as GuestAccessToken);
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  async function disable() {
    if (!window.confirm(`Vô hiệu hóa khách ${guest.fullName}?`)) {
      return;
    }
    setBusy(true);
    setError("");
    try {
      const response = await authenticatedFetch(
        `/api/memories/${memoryId}/guests/${guest.id}/disable`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ version: guest.version }),
        },
      );
      if (!response.ok) {
        throw new Error((await readProblem(response)).detail);
      }
      await onDisabled();
    } catch (reason) {
      setError(errorMessage(reason));
      setBusy(false);
    }
  }

  async function copyInvitation() {
    if (!issuedInvitation) {
      return;
    }
    await navigator.clipboard.writeText(issuedInvitation.url);
    setCopied(true);
  }

  return (
    <article className="guest-card">
      <header>
        <div>
          <strong>{guest.fullName}</strong>
          <span>
            {guest.status} · party tối đa {guest.maxPartySize}
            {guest.tokenIssued ? " · đã phát hành link" : " · chưa có link"}
          </span>
        </div>
        <span className="status-badge">{guest.status}</span>
      </header>

      <form className="guest-card-form" onSubmit={update}>
        <label>
          Tên khách
          <input
            value={fullName}
            onChange={(event) => setFullName(event.target.value)}
            maxLength={200}
            disabled={busy}
            required
          />
        </label>
        <label>
          Email
          <input
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            maxLength={255}
            disabled={busy}
          />
        </label>
        <label>
          Điện thoại
          <input
            value={phone}
            onChange={(event) => setPhone(event.target.value)}
            maxLength={16}
            pattern="\+[1-9][0-9]{1,14}"
            disabled={busy}
          />
        </label>
        <label>
          Nhóm
          <input
            value={guestGroup}
            onChange={(event) => setGuestGroup(event.target.value)}
            maxLength={100}
            disabled={busy}
          />
        </label>
        <label>
          Party size
          <input
            type="number"
            value={maxPartySize}
            onChange={(event) => setMaxPartySize(Number(event.target.value))}
            min={1}
            max={50}
            disabled={busy}
            required
          />
        </label>
        <label className="guest-note-field">
          Ghi chú nội bộ
          <textarea
            value={note}
            onChange={(event) => setNote(event.target.value)}
            maxLength={1000}
            rows={2}
            disabled={busy}
          />
        </label>
        <div className="guest-actions">
          <button type="submit" disabled={busy}>
            Lưu khách
          </button>
          {guest.status === "ACTIVE" ? (
            <>
            <button type="button" disabled={busy} onClick={() => void issue()}>
              {guest.tokenIssued ? "Phát hành lại link" : "Phát hành link"}
            </button>
            <button
              className="danger-button"
              type="button"
              disabled={busy}
              onClick={() => void disable()}
            >
              Vô hiệu hóa
            </button>
            </>
          ) : null}
        </div>
      </form>

      {issuedInvitation ? (
        <div className="issued-invitation" aria-live="polite">
          <p>
            Link này chỉ hiển thị trong lần phát hành hiện tại. Hãy sao chép ngay.
          </p>
          <input value={issuedInvitation.url} readOnly aria-label="Link mời mới" />
          <button type="button" onClick={() => void copyInvitation()}>
            {copied ? "Đã sao chép" : "Sao chép link"}
          </button>
        </div>
      ) : null}

      {error ? (
        <p className="form-note form-error" role="alert">
          {error}
        </p>
      ) : null}
    </article>
  );
}

async function requestGuests(memoryId: string, page: number) {
  const query = new URLSearchParams({ page: page.toString(), size: "20" });
  const response = await authenticatedFetch(
    `/api/memories/${memoryId}/guests?${query.toString()}`,
    { cache: "no-store" },
  );
  if (!response.ok) {
    throw new Error((await readProblem(response)).detail);
  }
  return (await response.json()) as MemoryGuestPage;
}

async function readProblem(response: Response) {
  try {
    const problem = (await response.json()) as Problem;
    return {
      code: problem.code,
      detail: problem.detail ?? "Không thể cập nhật khách mời.",
    };
  } catch {
    return { detail: "Không thể cập nhật khách mời." };
  }
}

function optional(value: string) {
  return value.trim() || null;
}

function errorMessage(reason: unknown) {
  return reason instanceof Error
    ? reason.message
    : "Dịch vụ tạm thời không khả dụng.";
}
