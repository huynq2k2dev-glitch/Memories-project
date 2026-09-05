"use client";

import { useState } from "react";

import { authenticatedFetch } from "@/lib/auth-session";

type LifecycleMemory = {
  id: string;
  status: "DRAFT" | "PUBLISHED" | "ARCHIVED";
  version: number;
};

export type ArchiveResult = {
  id: string;
  status: "ARCHIVED";
  updatedAt: string;
  version: number;
};

type Problem = {
  detail?: string;
};

export default function MemoryLifecycleEditor({
  memory,
  canArchive,
  canDelete,
  onArchived,
  onDeleted,
}: {
  memory: LifecycleMemory;
  canArchive: boolean;
  canDelete: boolean;
  onArchived: (result: ArchiveResult) => void;
  onDeleted: () => void;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function archive() {
    setBusy(true);
    setError("");
    try {
      const response = await authenticatedFetch(
        `/api/memories/${memory.id}/archive`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ version: memory.version }),
        },
      );
      if (!response.ok) {
        throw new Error(await problemDetail(response));
      }
      onArchived((await response.json()) as ArchiveResult);
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  async function softDelete() {
    if (!window.confirm("Xóa kỷ niệm này khỏi danh sách của bạn?")) {
      return;
    }
    setBusy(true);
    setError("");
    try {
      const response = await authenticatedFetch(
        `/api/memories/${memory.id}?version=${memory.version}`,
        { method: "DELETE" },
      );
      if (!response.ok) {
        throw new Error(await problemDetail(response));
      }
      onDeleted();
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  if (!canArchive && !canDelete) {
    return null;
  }

  return (
    <section className="lifecycle-editor">
      <h3>Quản lý kỷ niệm</h3>
      <p className="form-note">
        Lưu trữ để ngừng chia sẻ trang. Xóa để gỡ kỷ niệm khỏi danh sách của bạn.
      </p>
      <div className="lifecycle-actions">
        {canArchive ? (
          <button type="button" disabled={busy} onClick={() => void archive()}>
            {busy ? "Đang xử lý…" : "Lưu trữ kỷ niệm"}
          </button>
        ) : null}
        {canDelete ? (
          <button
            className="danger-button"
            type="button"
            disabled={busy}
            onClick={() => void softDelete()}
          >
            {busy ? "Đang xử lý…" : "Xóa kỷ niệm"}
          </button>
        ) : null}
      </div>
      {error ? (
        <p className="form-note form-error" role="alert">
          {error}
        </p>
      ) : null}
    </section>
  );
}

async function problemDetail(response: Response) {
  try {
    const problem = (await response.json()) as Problem;
    return problem.detail ?? "Không thể cập nhật vòng đời memory.";
  } catch {
    return "Không thể cập nhật vòng đời memory.";
  }
}

function errorMessage(reason: unknown) {
  return reason instanceof Error
    ? reason.message
    : "Dịch vụ tạm thời không khả dụng.";
}
