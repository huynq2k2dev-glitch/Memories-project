"use client";

import { useState } from "react";

import { authenticatedFetch } from "@/lib/auth-session";
import {
  type MemoryRenderPayload,
  RegisteredTemplateRenderer,
  supportsTemplateRenderer,
} from "@/templates/registry";

type PublishingMemory = {
  id: string;
  slug: string;
  status: "DRAFT" | "PUBLISHED" | "ARCHIVED";
  visibility: "PRIVATE" | "UNLISTED" | "PUBLIC" | "PASSWORD_PROTECTED";
  version: number;
};

type PublishResult = {
  id: string;
  slug: string;
  status: "PUBLISHED";
  visibility: "PRIVATE" | "UNLISTED" | "PUBLIC" | "PASSWORD_PROTECTED";
  publishedAt: string;
  version: number;
};

type Problem = {
  code?: string;
  detail?: string;
};

export default function MemoryPublishingEditor({
  memory,
  canPublish,
  onPublished,
}: {
  memory: PublishingMemory;
  canPublish: boolean;
  onPublished: (result: PublishResult) => void;
}) {
  const [preview, setPreview] = useState<MemoryRenderPayload | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function loadPreview() {
    setBusy(true);
    setError("");
    try {
      const response = await authenticatedFetch(
        `/api/memories/${memory.id}/preview`,
        { cache: "no-store" },
      );
      if (!response.ok) {
        throw new Error(await problemDetail(response));
      }
      setPreview((await response.json()) as MemoryRenderPayload);
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  async function publish() {
    if (!window.confirm("Xuất bản với quyền riêng tư hiện tại? Sau khi xuất bản, bạn sẽ không thể chỉnh sửa nội dung bản nháp.")) return;
    setBusy(true);
    setError("");
    try {
      const response = await authenticatedFetch(
        `/api/memories/${memory.id}/publish`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ version: memory.version }),
        },
      );
      if (!response.ok) {
        throw new Error(await problemDetail(response));
      }
      const result = (await response.json()) as PublishResult;
      onPublished(result);
      const previewResponse = await authenticatedFetch(
        `/api/memories/${memory.id}/preview`,
        { cache: "no-store" },
      );
      if (previewResponse.ok) {
        setPreview((await previewResponse.json()) as MemoryRenderPayload);
      }
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  const rendererSupported = preview
    ? supportsTemplateRenderer(preview.componentKey, preview.rendererVersion)
    : false;
  return (
    <section className="publishing-editor">
      <h3>Trang của bạn đã sẵn sàng chưa?</h3>
      <p className="form-note">
        Lưu các phần vừa chỉnh sửa, sau đó xem trước. Khi xuất bản, nội dung sẽ được khóa chỉnh sửa.
      </p>
      <div className="publishing-actions">
        <button type="button" disabled={busy} onClick={() => void loadPreview()}>
          {busy ? "Đang xử lý…" : "Xem trước nội dung đã lưu"}
        </button>
        {memory.status === "DRAFT" ? (
          canPublish ? (
            <button
              type="button"
              disabled={busy || !preview || !rendererSupported}
              onClick={() => void publish()}
            >
              {memory.visibility === "PRIVATE" ? "Hoàn tất và giữ riêng tư" : "Xuất bản trang"}
            </button>
          ) : null
        ) : memory.status === "PUBLISHED" && memory.visibility !== "PRIVATE" ? (
          <a
            href={`/memories/${encodeURIComponent(memory.slug)}`}
            target="_blank"
            rel="noreferrer"
          >
            Mở trang chia sẻ
          </a>
        ) : memory.status === "PUBLISHED" ? (
          <span className="form-note">
            Trang đang riêng tư. Người có quyền có thể xem bằng nút xem trước.
          </span>
        ) : (
          <span className="form-note">
            Kỷ niệm đã lưu trữ và không còn được chia sẻ công khai.
          </span>
        )}
      </div>
      {error ? (
        <p className="form-note form-error" role="alert">
          {error}
        </p>
      ) : null}
      {preview && !rendererSupported ? (
        <p className="form-note form-error" role="alert">
          Mẫu này chưa thể hiển thị. Vui lòng thử lại sau hoặc liên hệ hỗ trợ.
        </p>
      ) : null}
      {preview && rendererSupported ? (
        <div className="memory-live-preview">
          <RegisteredTemplateRenderer
            componentKey={preview.componentKey}
            rendererVersion={preview.rendererVersion}
            payload={preview}
          />
        </div>
      ) : null}
    </section>
  );
}

async function problemDetail(response: Response) {
  try {
    const problem = (await response.json()) as Problem;
    if (problem.code === "MEMORY_REQUIRED_SECTIONS_INCOMPLETE") {
      return "Mẫu còn thiếu nội dung bắt buộc. Mở phần Hoàn thiện câu chuyện, thêm nội dung và bật hiển thị cho các phần cần thiết.";
    }
    if (problem.code === "MEMORY_COVER_REQUIRED") {
      return "Mẫu này cần ảnh bìa. Mở phần Ảnh và khoảnh khắc, chọn một ảnh rồi bấm Dùng làm ảnh bìa.";
    }
    if (problem.code === "MEMORY_VERSION_CONFLICT") {
      return "Kỷ niệm vừa được cập nhật. Hãy làm mới thông tin và xem trước lại trước khi xuất bản.";
    }
    return problem.detail ?? "Chưa thể xuất bản trang. Vui lòng thử lại.";
  } catch {
    return "Chưa thể xuất bản trang. Vui lòng thử lại.";
  }
}

function errorMessage(reason: unknown) {
  return reason instanceof Error
    ? reason.message
    : "Dịch vụ tạm thời không khả dụng.";
}
