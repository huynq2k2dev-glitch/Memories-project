"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import RequireAuth from "@/components/require-auth";
import { authenticatedFetch } from "@/lib/auth-session";
import {
  type MemoryDetail,
  MemoryWorkspace,
} from "@/app/templates/template-catalog-client";

export default function MemoryEditClient({ memoryId }: { memoryId: string }) {
  return (
    <RequireAuth>
      <EditorContent memoryId={memoryId} />
    </RequireAuth>
  );
}

function EditorContent({ memoryId }: { memoryId: string }) {
  const [memory, setMemory] = useState<MemoryDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    async function load() {
      setLoading(true);
      setError("");
      try {
        const response = await authenticatedFetch(
          `/api/memories/${encodeURIComponent(memoryId)}`,
          { cache: "no-store" },
        );
        if (!response.ok) {
          const problem = (await response.json()) as { code?: string };
          throw new Error(editorError(response.status, problem.code));
        }
        if (active) {
          setMemory((await response.json()) as MemoryDetail);
        }
      } catch (reason) {
        if (active) {
          setError(
            reason instanceof Error
              ? reason.message
              : "Chưa thể mở memory để chỉnh sửa.",
          );
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    void load();
    return () => {
      active = false;
    };
  }, [memoryId]);

  return (
    <main className="page-shell editor-page">
      <header className="page-heading compact-heading">
        <div>
          <p className="eyebrow">Biên soạn</p>
          <h1>{memory?.title ?? "Biên soạn kỷ niệm"}</h1>
        </div>
        <Link className="secondary-link" href="/memories">Kỷ niệm của tôi</Link>
      </header>
      {loading ? <div className="loading-panel">Đang tải memory…</div> : null}
      {error ? <p className="error-panel" role="alert">{error}</p> : null}
      {memory ? <MemoryWorkspace key={`${memory.id}-${memory.version}`} initialMemory={memory} /> : null}
    </main>
  );
}

function editorError(status: number, code: string | undefined) {
  if (status === 404 || code === "MEMORY_NOT_FOUND") {
    return "Không tìm thấy memory hoặc bạn không có quyền truy cập.";
  }
  if (status === 403 || code === "ACCESS_DENIED") {
    return "Bạn không có quyền chỉnh sửa memory này.";
  }
  if (code === "MEMORY_NOT_EDITABLE") {
    return "Memory này không còn ở trạng thái có thể chỉnh sửa.";
  }
  return "Chưa thể mở memory để chỉnh sửa.";
}
