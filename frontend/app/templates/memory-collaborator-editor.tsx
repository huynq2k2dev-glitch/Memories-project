"use client";

import { type FormEvent, useCallback, useEffect, useState } from "react";

import { authenticatedFetch } from "@/lib/auth-session";

type CollaboratorPermission = "VIEW" | "EDIT" | "ADMIN";

type MemoryCollaborator = {
  id: string;
  userId: string;
  displayName: string | null;
  accountActive: boolean;
  permission: CollaboratorPermission;
  status: "ACTIVE" | "REVOKED";
  invitedBy: string;
  createdAt: string;
  updatedAt: string;
  revokedAt: string | null;
};

type Problem = {
  detail?: string;
};

export default function MemoryCollaboratorEditor({
  memoryId,
  onChanged,
}: {
  memoryId: string;
  onChanged: () => Promise<void>;
}) {
  const [collaborators, setCollaborators] = useState<MemoryCollaborator[]>([]);
  const [email, setEmail] = useState("");
  const [permission, setPermission] = useState<CollaboratorPermission>("VIEW");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setBusy(true);
    setError("");
    try {
      setCollaborators(await requestCollaborators(memoryId));
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }, [memoryId]);

  useEffect(() => {
    let active = true;
    void requestCollaborators(memoryId)
      .then((loadedCollaborators) => {
        if (active) {
          setCollaborators(loadedCollaborators);
        }
      })
      .catch((reason: unknown) => {
        if (active) {
          setError(errorMessage(reason));
        }
      });
    return () => {
      active = false;
    };
  }, [memoryId]);

  async function add(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const response = await authenticatedFetch(
        `/api/memories/${memoryId}/collaborators`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ email, permission }),
        },
      );
      if (!response.ok) {
        throw new Error(await problemDetail(response));
      }
      setEmail("");
      await load();
      await onChanged();
    } catch (reason) {
      setError(errorMessage(reason));
      setBusy(false);
    }
  }

  async function changePermission(
    collaboratorId: string,
    nextPermission: CollaboratorPermission,
  ) {
    setBusy(true);
    setError("");
    try {
      const response = await authenticatedFetch(
        `/api/memories/${memoryId}/collaborators/${collaboratorId}`,
        {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ permission: nextPermission }),
        },
      );
      if (!response.ok) {
        throw new Error(await problemDetail(response));
      }
      await load();
      await onChanged();
    } catch (reason) {
      setError(errorMessage(reason));
      setBusy(false);
    }
  }

  async function revoke(collaboratorId: string) {
    setBusy(true);
    setError("");
    try {
      const response = await authenticatedFetch(
        `/api/memories/${memoryId}/collaborators/${collaboratorId}`,
        { method: "DELETE" },
      );
      if (!response.ok) {
        throw new Error(await problemDetail(response));
      }
      await load();
      await onChanged();
    } catch (reason) {
      setError(errorMessage(reason));
      setBusy(false);
    }
  }

  return (
    <section className="collaborator-editor">
      <header>
        <div>
          <h3>Cộng tác viên</h3>
          <p className="form-note">
            Chỉ thêm tài khoản đã kích hoạt. Mời lại email đã thu hồi sẽ kích hoạt
            lại quan hệ cũ.
          </p>
        </div>
        <button type="button" disabled={busy} onClick={() => void load()}>
          Tải lại
        </button>
      </header>

      <form className="collaborator-form" onSubmit={add}>
        <label htmlFor={`collaborator-email-${memoryId}`}>Email tài khoản</label>
        <input
          id={`collaborator-email-${memoryId}`}
          type="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          maxLength={320}
          required
        />
        <label htmlFor={`collaborator-permission-${memoryId}`}>Quyền</label>
        <select
          id={`collaborator-permission-${memoryId}`}
          value={permission}
          onChange={(event) =>
            setPermission(event.target.value as CollaboratorPermission)
          }
        >
          <option value="VIEW">Chỉ xem</option>
          <option value="EDIT">Biên soạn nội dung</option>
          <option value="ADMIN">Quản lý kỷ niệm này</option>
        </select>
        <button type="submit" disabled={busy}>
          {busy ? "Đang xử lý…" : "Thêm cộng tác viên"}
        </button>
      </form>

      {error ? (
        <p className="form-note form-error" role="alert">
          {error}
        </p>
      ) : null}

      <div className="collaborator-list">
        {collaborators.length === 0 && !busy ? (
          <p className="form-note">Chưa có cộng tác viên.</p>
        ) : null}
        {collaborators.map((collaborator) => (
          <article className="collaborator-card" key={collaborator.id}>
            <div>
              <strong>{collaborator.displayName ?? "Người cùng cộng tác"}</strong>
              <span>
                {collaborator.status === "ACTIVE" ? "Đang có quyền truy cập" : "Đã thu hồi quyền"}
                {!collaborator.accountActive ? " · tài khoản chưa hoạt động" : ""}
              </span>
              {collaborator.revokedAt ? (
                <span>Thu hồi lúc {formatInstant(collaborator.revokedAt)}</span>
              ) : null}
            </div>
            {collaborator.status === "ACTIVE" ? (
              <div className="collaborator-actions">
                <select
                  aria-label={`Quyền của ${collaborator.displayName ?? collaborator.userId}`}
                  value={collaborator.permission}
                  disabled={busy}
                  onChange={(event) =>
                    void changePermission(
                      collaborator.id,
                      event.target.value as CollaboratorPermission,
                    )
                  }
                >
                  <option value="VIEW">Chỉ xem</option>
                  <option value="EDIT">Biên soạn nội dung</option>
                  <option value="ADMIN">Quản lý kỷ niệm này</option>
                </select>
                <button
                  className="danger-button"
                  type="button"
                  disabled={busy}
                  onClick={() => void revoke(collaborator.id)}
                >
                  Thu hồi
                </button>
              </div>
            ) : (
              <span className="status-badge">Đã thu hồi</span>
            )}
          </article>
        ))}
      </div>
    </section>
  );
}

async function problemDetail(response: Response) {
  try {
    const problem = (await response.json()) as Problem;
    return problem.detail ?? "Không thể cập nhật cộng tác viên.";
  } catch {
    return "Không thể cập nhật cộng tác viên.";
  }
}

async function requestCollaborators(memoryId: string) {
  const response = await authenticatedFetch(
    `/api/memories/${memoryId}/collaborators`,
    { cache: "no-store" },
  );
  if (!response.ok) {
    throw new Error(await problemDetail(response));
  }
  return (await response.json()) as MemoryCollaborator[];
}

function errorMessage(reason: unknown) {
  return reason instanceof Error
    ? reason.message
    : "Dịch vụ tạm thời không khả dụng.";
}

function formatInstant(value: string) {
  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}
