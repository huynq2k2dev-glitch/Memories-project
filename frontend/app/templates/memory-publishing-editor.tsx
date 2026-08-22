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
  visibility: "PRIVATE" | "UNLISTED" | "PUBLIC";
  version: number;
};

type PublishResult = {
  id: string;
  slug: string;
  status: "PUBLISHED";
  visibility: "UNLISTED" | "PUBLIC";
  publishedAt: string;
  version: number;
};

type Problem = {
  detail?: string;
};

export default function MemoryPublishingEditor({
  memory,
  onPublished,
}: {
  memory: PublishingMemory;
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
  const publishableVisibility =
    memory.visibility === "PUBLIC" || memory.visibility === "UNLISTED";

  return (
    <section className="publishing-editor">
      <h3>Preview và publish</h3>
      <p className="form-note">
        Preview và trang public dùng cùng payload cùng renderer đã đăng ký.
      </p>
      <div className="publishing-actions">
        <button type="button" disabled={busy} onClick={() => void loadPreview()}>
          {busy ? "Đang xử lý…" : "Tải preview"}
        </button>
        {memory.status === "DRAFT" ? (
          <button
            type="button"
            disabled={busy || !publishableVisibility}
            onClick={() => void publish()}
          >
            Publish {memory.visibility}
          </button>
        ) : (
          <a
            href={`/memories/${encodeURIComponent(memory.slug)}`}
            target="_blank"
            rel="noreferrer"
          >
            Mở trang public
          </a>
        )}
      </div>
      {!publishableVisibility && memory.status === "DRAFT" ? (
        <p className="form-note">
          Lưu visibility thành PUBLIC hoặc UNLISTED trước khi publish. PRIVATE và
          PASSWORD_PROTECTED thuộc Ticket 15.
        </p>
      ) : null}
      {error ? (
        <p className="form-note form-error" role="alert">
          {error}
        </p>
      ) : null}
      {preview && !rendererSupported ? (
        <p className="form-note form-error" role="alert">
          Renderer {preview.componentKey}@{preview.rendererVersion} không có trong
          frontend build.
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
    return problem.detail ?? "Không thể xử lý yêu cầu publish.";
  } catch {
    return "Không thể xử lý yêu cầu publish.";
  }
}

function errorMessage(reason: unknown) {
  return reason instanceof Error
    ? reason.message
    : "Dịch vụ tạm thời không khả dụng.";
}
