"use client";

import { useRouter } from "next/navigation";
import { type FormEvent, useState } from "react";

import RequireAuth from "@/components/require-auth";
import type { MemoryType } from "@/lib/api-types";
import { authenticatedFetch } from "@/lib/auth-session";
import { MEMORY_TYPES, memoryTypeLabel } from "@/lib/memory-labels";
import { occasionDesign, sectionLabel } from "@/lib/occasion-design";
import { bookPreview } from "@/templates/book-preview";
import {
  type MemoryRenderPayload,
  RegisteredTemplateRenderer,
  supportsTemplateRenderer,
} from "@/templates/registry";

type PublishedTemplateVersion = {
  book?: MemoryRenderPayload["book"];
  id: string;
  versionNo: number;
  componentKey: string;
  rendererVersion: string;
  coverRequired: boolean;
  defaultConfig: Record<string, unknown>;
  allowedSectionTypes: string[];
  requiredSections: string[];
};

type TemplateCatalogItem = {
  id: string;
  code: string;
  name: string;
  memoryType: MemoryType;
  description: string | null;
  versions: PublishedTemplateVersion[];
};

type TemplateCatalogPage = {
  items: TemplateCatalogItem[];
};

type SelectedTemplate = {
  template: TemplateCatalogItem;
  version: PublishedTemplateVersion;
};

export default function NewMemoryWizard() {
  return (
    <RequireAuth>
      <WizardContent />
    </RequireAuth>
  );
}

function WizardContent() {
  const router = useRouter();
  const [step, setStep] = useState(1);
  const [memoryType, setMemoryType] = useState<MemoryType | null>(null);
  const [templates, setTemplates] = useState<TemplateCatalogItem[]>([]);
  const [selected, setSelected] = useState<SelectedTemplate | null>(null);
  const [title, setTitle] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function loadTemplates() {
    if (!memoryType) {
      return;
    }
    setBusy(true);
    setError("");
    try {
      const query = new URLSearchParams({
        page: "0",
        size: "50",
        status: "ACTIVE",
        memoryType,
      });
      const response = await authenticatedFetch(`/api/templates?${query}`, {
        cache: "no-store",
      });
      if (!response.ok) {
        throw new Error("Chưa thể tải template cho loại memory này.");
      }
      const payload = (await response.json()) as TemplateCatalogPage;
      const available = payload.items.map((template) => ({
        ...template,
        versions: template.versions.filter((version) => supportsTemplateRenderer(version.componentKey, version.rendererVersion)),
      })).filter((template) => template.versions.length > 0);
      setTemplates(available);
      const firstTemplate = available[0];
      const firstVersion = firstTemplate?.versions[0];
      setSelected(
        firstTemplate && firstVersion
          ? { template: firstTemplate, version: firstVersion }
          : null,
      );
      setStep(2);
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selected || !memoryType || busy) {
      return;
    }
    setBusy(true);
    setError("");
    try {
      const response = await authenticatedFetch("/api/memories", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          memoryType,
          templateVersionId: selected.version.id,
          title: title.trim(),
        }),
      });
      if (!response.ok) {
        const problem = (await response.json()) as { code?: string; detail?: string };
        if (
          problem.code === "TEMPLATE_VERSION_NOT_SELECTABLE" ||
          problem.code === "TEMPLATE_NOT_FOUND"
        ) {
          setStep(2);
          setSelected(null);
          throw new Error("Template vừa chọn không còn khả dụng. Vui lòng chọn lại.");
        }
        throw new Error(problem.detail ?? "Không thể tạo memory.");
      }
      const memory = (await response.json()) as { id: string };
      router.push(`/memories/${memory.id}/edit`);
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="page-shell wizard-page">
      <header className="wizard-heading">
        <p className="eyebrow">Một nơi dành cho những điều đáng nhớ</p>
        <h1>Bắt đầu một câu chuyện mới</h1>
        <p>Chọn dịp, tìm một mẫu bạn thích và bắt đầu bằng một cái tên. Bạn có thể thêm ảnh và viết tiếp sau.</p>
        <ol className="wizard-progress" aria-label="Tiến trình tạo kỷ niệm">
          {["Chọn loại", "Chọn mẫu", "Đặt tên"].map((label, index) => (
            <li aria-current={step === index + 1 ? "step" : undefined} className={step === index + 1 ? "current" : step > index + 1 ? "complete" : ""} key={label}>
              <span>{index + 1}</span>{label}
            </li>
          ))}
        </ol>
      </header>

      {error ? <p className="error-panel" role="alert">{error}</p> : null}

      {step === 1 ? (
        <section className="wizard-panel" aria-labelledby="memory-type-title">
          <h2 id="memory-type-title">Bạn đang muốn lưu giữ dịp nào?</h2>
          <div className="choice-grid">
            {MEMORY_TYPES.map((type) => (
              <label className={memoryType === type ? "choice-card selected" : "choice-card"} key={type}>
                <input type="radio" name="memoryType" value={type} checked={memoryType === type} onChange={() => setMemoryType(type)} />
                <strong>{memoryTypeLabel(type)}</strong>
                <span className="occasion-symbol" aria-hidden="true">{occasionDesign(type).symbol}</span>
                <span>{occasionDesign(type).description}</span>
              </label>
            ))}
          </div>
          <div className="wizard-actions">
            <button type="button" disabled={!memoryType || busy} onClick={() => void loadTemplates()}>
              {busy ? "Đang tải mẫu…" : "Tiếp tục"}
            </button>
          </div>
        </section>
      ) : null}

      {step === 2 ? (
        <section className="wizard-panel" aria-labelledby="template-title">
          <h2 id="template-title">Chọn phong cách gần với bạn nhất</h2>
          {templates.length === 0 ? (
            <div className="empty-state compact">
              <p>Chưa có mẫu sẵn sàng cho dịp này. Bạn có thể chọn một dịp khác.</p>
              <button type="button" onClick={() => setStep(1)}>Chọn loại khác</button>
            </div>
          ) : (
            <div className="template-picker">
              <div className="template-choice-list">
                {templates.flatMap((template) =>
                  template.versions.map((version) => (
                    <button
                      className={selected?.version.id === version.id ? "template-choice selected" : "template-choice"}
                      type="button"
                      key={version.id}
                      onClick={() => setSelected({ template, version })}
                    >
                      <strong>{template.name}</strong>
                      <span>{template.description ?? occasionDesign(template.memoryType).description}</span>
                      {template.versions.length > 1 ? <small>Lựa chọn {template.versions.indexOf(version) + 1}</small> : null}
                    </button>
                  )),
                )}
              </div>
              <div className="template-live-preview" aria-live="polite">
                <p className="form-note">Nội dung minh họa · Bạn sẽ thay bằng câu chuyện của mình</p>
                {selected && supportsTemplateRenderer(selected.version.componentKey, selected.version.rendererVersion) ? (
                  <RegisteredTemplateRenderer
                    componentKey={selected.version.componentKey}
                    rendererVersion={selected.version.rendererVersion}
                    payload={previewPayload(selected)}
                  />
                ) : (
                  <p>Chọn một mẫu để xem trước.</p>
                )}
              </div>
            </div>
          )}
          <div className="wizard-actions split">
            <button className="secondary-button" type="button" onClick={() => setStep(1)}>Quay lại</button>
            <button type="button" disabled={!selected} onClick={() => setStep(3)}>Tiếp tục</button>
          </div>
        </section>
      ) : null}

      {step === 3 && selected && memoryType ? (
        <section className="wizard-panel" aria-labelledby="memory-title-heading">
          <h2 id="memory-title-heading">{occasionDesign(memoryType).titleLabel}</h2>
          <form className="wizard-review" onSubmit={create}>
            <label htmlFor="new-memory-title">Tiêu đề</label>
            <input id="new-memory-title" value={title} onChange={(event) => setTitle(event.target.value)} placeholder={occasionDesign(memoryType).example} maxLength={255} autoFocus required />
            <p className="form-note">Chỉ cần một tiêu đề để bắt đầu. Bước tiếp theo sẽ hướng dẫn bạn thêm lời kể và ảnh.</p>
            {selected.version.coverRequired ? <p className="form-note">Mẫu này cần một ảnh bìa trước khi xuất bản. Bạn sẽ thêm ảnh ở bước biên soạn.</p> : null}
            {selected.version.requiredSections.length > 0 ? <p className="form-note">Các phần cần hoàn thiện trước khi xuất bản: {selected.version.requiredSections.map(sectionLabel).join(", ")}.</p> : null}
            <dl>
              <div><dt>Loại</dt><dd>{memoryTypeLabel(memoryType)}</dd></div>
              <div><dt>Mẫu đã chọn</dt><dd>{selected.template.name}</dd></div>
            </dl>
            <div className="wizard-actions split">
              <button className="secondary-button" type="button" disabled={busy} onClick={() => setStep(2)}>Quay lại</button>
              <button type="submit" disabled={busy || !title.trim()}>{busy ? "Đang tạo…" : "Tạo bản nháp và viết tiếp"}</button>
            </div>
          </form>
        </section>
      ) : null}
    </main>
  );
}

function previewPayload(selected: SelectedTemplate): MemoryRenderPayload {
  if (selected.version.book) return {
    ...bookPreview(selected.version.book, selected.version.defaultConfig),
    templateVersionId: selected.version.id,
    memoryType: selected.template.memoryType,
    componentKey: selected.version.componentKey,
    rendererVersion: selected.version.rendererVersion,
  };
  return {
    slug: "template-preview",
    title: occasionDesign(selected.template.memoryType).example,
    memoryType: selected.template.memoryType,
    status: "DRAFT",
    visibility: "PRIVATE",
    summary: occasionDesign(selected.template.memoryType).sample,
    themeConfig: selected.version.defaultConfig,
    eventStartAt: null,
    publishedAt: null,
    expiresAt: null,
    templateVersionId: selected.version.id,
    book: selected.version.book,
    componentKey: selected.version.componentKey,
    rendererVersion: selected.version.rendererVersion,
    cover: null,
    members: [],
    sections: [],
    locations: [],
    events: [],
    images: [],
    messages: [],
  };
}

function errorMessage(reason: unknown) {
  return reason instanceof Error ? reason.message : "Dịch vụ tạm thời không khả dụng.";
}
