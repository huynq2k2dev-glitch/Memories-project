import { notFound } from "next/navigation";

import {
  type MemoryRenderPayload,
  RegisteredTemplateRenderer,
  supportsTemplateRenderer,
} from "@/templates/registry";

const BACKEND_URL = process.env.BACKEND_URL ?? "http://127.0.0.1:8080";

export default async function PublicMemoryPage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  const response = await fetch(
    `${BACKEND_URL}/api/v1/public/memories/${encodeURIComponent(slug)}`,
    { cache: "no-store" },
  );
  if (response.status === 404) {
    notFound();
  }
  if (!response.ok) {
    throw new Error("Public memory is temporarily unavailable.");
  }
  const payload = (await response.json()) as MemoryRenderPayload;

  if (!supportsTemplateRenderer(payload.componentKey, payload.rendererVersion)) {
    return (
      <main className="public-memory-shell">
        <h1>Renderer chưa tương thích</h1>
        <p>Frontend build hiện tại chưa thể hiển thị phiên bản memory này.</p>
      </main>
    );
  }

  return (
    <main className="public-memory-shell">
      <RegisteredTemplateRenderer
        componentKey={payload.componentKey}
        rendererVersion={payload.rendererVersion}
        payload={payload}
      />
    </main>
  );
}
