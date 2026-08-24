"use client";

import Link from "next/link";
import { type FormEvent, useEffect, useState } from "react";

import { authenticatedFetch } from "@/lib/auth-session";
import {
  type MemoryRenderPayload,
  RegisteredTemplateRenderer,
  supportsTemplateRenderer,
} from "@/templates/registry";
import MemoryCollaboratorEditor from "./memory-collaborator-editor";
import MemoryContentEditor from "./memory-content-editor";
import MemoryGuestEditor from "./memory-guest-editor";
import MemoryMessageEditor from "./memory-message-editor";
import MemoryMediaEditor from "./memory-media-editor";
import MemoryLifecycleEditor from "./memory-lifecycle-editor";
import MemoryPublishingEditor from "./memory-publishing-editor";
import MemoryScheduleEditor from "./memory-schedule-editor";
import MemoryShareLinkEditor from "./memory-share-link-editor";

const MEMORY_TYPES = [
  "",
  "WEDDING",
  "FUNERAL",
  "GRADUATION",
  "HOUSEWARMING",
  "PERSONAL",
] as const;

type MemoryType = (typeof MEMORY_TYPES)[number];

type PublishedTemplateVersion = {
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
  memoryType: Exclude<MemoryType, "">;
  description: string | null;
  versions: PublishedTemplateVersion[];
};

type TemplateCatalogPage = {
  items: TemplateCatalogItem[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};

type SelectedTemplate = {
  template: TemplateCatalogItem;
  version: PublishedTemplateVersion;
};

type MemoryDetail = {
  id: string;
  ownerId: string;
  templateVersionId: string;
  slug: string;
  title: string;
  memoryType: Exclude<MemoryType, "">;
  status: "DRAFT" | "PUBLISHED" | "ARCHIVED";
  visibility: "PRIVATE" | "UNLISTED" | "PUBLIC" | "PASSWORD_PROTECTED";
  summary: string | null;
  themeConfig: Record<string, unknown>;
  settings: Record<string, unknown>;
  coverAssetId: string | null;
  eventStartAt: string | null;
  publishedAt: string | null;
  expiresAt: string | null;
  updatedAt: string;
  version: number;
  allowedSectionTypes: string[];
  capabilities: {
    owner: boolean;
    collaboratorPermission: "VIEW" | "EDIT" | "ADMIN" | null;
    canEdit: boolean;
    canPublish: boolean;
    canManageCollaborators: boolean;
    canChangeAccessPolicy: boolean;
    canManageGuests: boolean;
    canArchive: boolean;
    canDelete: boolean;
  };
};

type MemoryProblem = {
  code?: string;
  detail: string;
};

export default function TemplateCatalogClient() {
  const [catalog, setCatalog] = useState<TemplateCatalogPage | null>(null);
  const [memoryType, setMemoryType] = useState<MemoryType>("");
  const [selected, setSelected] = useState<SelectedTemplate | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    void fetchCatalog(0, "")
      .then((result) => {
        if (active) {
          setCatalog(result);
          setSelected(firstSelection(result));
        }
      })
      .catch((reason: unknown) => {
        if (active) {
          setError(errorMessage(reason));
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, []);

  async function load(page: number, type: MemoryType = memoryType) {
    setLoading(true);
    setError("");
    try {
      const result = await fetchCatalog(page, type);
      setCatalog(result);
      setSelected(firstSelection(result));
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setLoading(false);
    }
  }

  const rendererSupported = selected
    ? supportsTemplateRenderer(
        selected.version.componentKey,
        selected.version.rendererVersion,
      )
    : false;

  return (
    <main className="catalog-shell">
      <section className="catalog-header">
        <div>
          <p className="eyebrow">Catalog</p>
          <h1>Chọn template</h1>
          <p className="summary">
            Chỉ các phiên bản đã publish thuộc template đang hoạt động được hiển thị.
          </p>
        </div>
        <Link className="secondary-link" href="/login">
          Đăng nhập
        </Link>
      </section>

      <section className="catalog-controls" aria-label="Lọc template">
        <label htmlFor="catalog-memory-type">Loại memory</label>
        <select
          id="catalog-memory-type"
          value={memoryType}
          onChange={(event) => setMemoryType(event.target.value as MemoryType)}
        >
          <option value="">Tất cả</option>
          {MEMORY_TYPES.filter(Boolean).map((value) => (
            <option key={value}>{value}</option>
          ))}
        </select>
        <button type="button" disabled={loading} onClick={() => void load(0)}>
          Áp dụng
        </button>
      </section>

      <OpenMemoryForm />

      {error ? (
        <p className="form-note form-error" role="alert">
          {error}
        </p>
      ) : null}

      <div className="catalog-layout" aria-busy={loading}>
        <section className="catalog-list" aria-label="Danh sách template">
          {loading ? <p>Đang tải template…</p> : null}
          {!loading && catalog?.items.length === 0 ? (
            <p>Không có template phù hợp.</p>
          ) : null}
          {catalog?.items.map((template) => (
            <article className="catalog-card" key={template.id}>
              <p className="template-code">{template.code}</p>
              <h2>{template.name}</h2>
              <p>{template.description ?? "Chưa có mô tả."}</p>
              <p className="catalog-meta">{template.memoryType}</p>
              <div className="catalog-version-actions">
                {template.versions.map((version) => (
                  <button
                    key={version.id}
                    type="button"
                    onClick={() => setSelected({ template, version })}
                  >
                    Chọn version {version.versionNo}
                  </button>
                ))}
              </div>
            </article>
          ))}

          {catalog && catalog.totalPages > 1 ? (
            <nav className="pagination" aria-label="Phân trang template">
              <button
                type="button"
                disabled={loading || catalog.page === 0}
                onClick={() => void load(catalog.page - 1)}
              >
                Trang trước
              </button>
              <span>
                Trang {catalog.page + 1}/{catalog.totalPages}
              </span>
              <button
                type="button"
                disabled={loading || catalog.page + 1 >= catalog.totalPages}
                onClick={() => void load(catalog.page + 1)}
              >
                Trang sau
              </button>
            </nav>
          ) : null}
        </section>

        <section className="catalog-preview" aria-live="polite">
          <h2>Preview lựa chọn</h2>
          {!selected ? <p>Chọn một template version để preview.</p> : null}
          {selected && !rendererSupported ? (
            <p className="form-note form-error" role="alert">
              Renderer {selected.version.componentKey}@
              {selected.version.rendererVersion} không tương thích với frontend build này.
            </p>
          ) : null}
          {selected && rendererSupported ? (
            <>
              <RegisteredTemplateRenderer
                componentKey={selected.version.componentKey}
                rendererVersion={selected.version.rendererVersion}
                payload={catalogPreviewPayload(selected)}
              />
              <dl className="contract-summary">
                <div>
                  <dt>Template version ID</dt>
                  <dd>{selected.version.id}</dd>
                </div>
                <div>
                  <dt>Required sections</dt>
                  <dd>{selected.version.requiredSections.join(", ") || "Không có"}</dd>
                </div>
                <div>
                  <dt>Cover bắt buộc</dt>
                  <dd>{selected.version.coverRequired ? "Có" : "Không"}</dd>
                </div>
              </dl>
              <p className="form-note">
                ID này sẽ được gửi khi tạo memory draft ở bước tiếp theo.
              </p>
              <CreateMemoryForm key={selected.version.id} selected={selected} />
            </>
          ) : null}
        </section>
      </div>
    </main>
  );
}

function CreateMemoryForm({ selected }: { selected: SelectedTemplate }) {
  const [title, setTitle] = useState("");
  const [memory, setMemory] = useState<MemoryDetail | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const response = await authenticatedFetch("/api/memories", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          templateVersionId: selected.version.id,
          memoryType: selected.template.memoryType,
          title,
        }),
      });
      if (!response.ok) {
        throw new Error((await readMemoryProblem(response)).detail);
      }
      setMemory((await response.json()) as MemoryDetail);
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="memory-draft-workspace">
      <form className="create-memory-form" onSubmit={create}>
        <h3>Tạo memory draft</h3>
        <label htmlFor="memory-title">Tiêu đề</label>
        <input
          id="memory-title"
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          maxLength={255}
          required
        />
        <button type="submit" disabled={busy}>
          {busy ? "Đang tạo…" : "Tạo memory"}
        </button>
        {error ? (
          <p className="form-note form-error" role="alert">
            {error}
          </p>
        ) : null}
      </form>
      {memory ? (
        <MemoryWorkspace
          key={`${memory.id}-${memory.version}`}
          initialMemory={memory}
        />
      ) : null}
    </div>
  );
}

function OpenMemoryForm() {
  const [memoryId, setMemoryId] = useState("");
  const [memory, setMemory] = useState<MemoryDetail | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function openExisting(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const response = await authenticatedFetch(
        `/api/memories/${encodeURIComponent(memoryId.trim())}`,
        { cache: "no-store" },
      );
      if (!response.ok) {
        throw new Error((await readMemoryProblem(response)).detail);
      }
      setMemory((await response.json()) as MemoryDetail);
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="memory-open-panel">
      <form className="create-memory-form" onSubmit={openExisting}>
        <h3>Mở memory hiện có</h3>
        <p className="form-note">
          Owner hoặc cộng tác viên nhập ID memory để mở đúng workspace theo quyền
          backend cấp.
        </p>
        <label htmlFor="existing-memory-id">Memory ID</label>
        <input
          id="existing-memory-id"
          value={memoryId}
          onChange={(event) => setMemoryId(event.target.value)}
          required
        />
        <button type="submit" disabled={busy}>
          {busy ? "Đang mở…" : "Mở memory"}
        </button>
        {error ? (
          <p className="form-note form-error" role="alert">
            {error}
          </p>
        ) : null}
      </form>
      {memory ? (
        <MemoryWorkspace
          key={`${memory.id}-${memory.version}`}
          initialMemory={memory}
        />
      ) : null}
    </section>
  );
}

function MemoryWorkspace({ initialMemory }: { initialMemory: MemoryDetail }) {
  const [memory, setMemory] = useState(initialMemory);
  const [deleted, setDeleted] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function reload() {
    setBusy(true);
    setError("");
    try {
      const response = await authenticatedFetch(`/api/memories/${memory.id}`, {
        cache: "no-store",
      });
      if (!response.ok) {
        throw new Error((await readMemoryProblem(response)).detail);
      }
      setMemory((await response.json()) as MemoryDetail);
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  if (deleted) {
    return (
      <div className="memory-created" aria-live="polite">
        Memory đã được xóa mềm. Dữ liệu được giữ lại cho chức năng khôi phục sau.
      </div>
    );
  }

  return (
    <div className="memory-draft-workspace">
      <div className="memory-created" aria-live="polite">
        <strong>{memory.title}</strong>
        <span>
          {memory.status} · {memory.visibility} · phiên bản {memory.version}
        </span>
        <span>
          Quyền: {memory.capabilities.owner
            ? "OWNER"
            : memory.capabilities.collaboratorPermission}
        </span>
        {memory.summary ? <p>{memory.summary}</p> : null}
        <code>{memory.slug}</code>
        <button type="button" disabled={busy} onClick={() => void reload()}>
          Đọc lại chi tiết quản trị
        </button>
      </div>
      {error ? (
        <p className="form-note form-error" role="alert">
          {error}
        </p>
      ) : null}
      {memory.capabilities.canManageCollaborators ? (
        <MemoryCollaboratorEditor memoryId={memory.id} onChanged={reload} />
      ) : null}
      {memory.capabilities.canManageCollaborators ? (
        <MemoryShareLinkEditor
          memoryId={memory.id}
          visibility={memory.visibility}
        />
      ) : null}
      {memory.capabilities.canManageGuests ? (
        <MemoryGuestEditor memoryId={memory.id} />
      ) : null}
      {memory.capabilities.canManageCollaborators ? (
        <MemoryMessageEditor
          memoryId={memory.id}
          memoryStatus={memory.status}
          memoryVersion={memory.version}
          moderationEnabled={memory.settings.messageModerationEnabled !== false}
          onMemoryChanged={reload}
        />
      ) : null}
      {memory.status === "DRAFT" && memory.capabilities.canEdit ? (
        <>
          <MemoryEditor
            key={`${memory.id}-${memory.version}`}
            memory={memory}
            canChangeAccessPolicy={memory.capabilities.canChangeAccessPolicy}
            onReload={reload}
            onUpdated={setMemory}
          />
          <MemoryContentEditor
            memoryId={memory.id}
            allowedSectionTypes={memory.allowedSectionTypes}
          />
          <MemoryScheduleEditor memoryId={memory.id} />
          <MemoryMediaEditor
            memoryId={memory.id}
            memoryVersion={memory.version}
            coverAssetId={memory.coverAssetId}
            onCoverUpdated={(result) =>
              setMemory((current) => ({
                ...current,
                coverAssetId: result.coverAssetId,
                version: result.version,
              }))
            }
          />
        </>
      ) : null}
      <MemoryPublishingEditor
        memory={memory}
        canPublish={memory.capabilities.canPublish}
        onPublished={(result) =>
          setMemory((current) => ({
            ...current,
            status: result.status,
            visibility: result.visibility,
            publishedAt: result.publishedAt,
            version: result.version,
          }))
        }
      />
      <MemoryLifecycleEditor
        memory={memory}
        canArchive={memory.capabilities.canArchive}
        canDelete={memory.capabilities.canDelete}
        onArchived={(result) =>
          setMemory((current) => ({
            ...current,
            status: result.status,
            updatedAt: result.updatedAt,
            version: result.version,
            capabilities: {
              ...current.capabilities,
              canArchive: false,
            },
          }))
        }
        onDeleted={() => setDeleted(true)}
      />
    </div>
  );
}

function MemoryEditor({
  memory,
  canChangeAccessPolicy,
  onReload,
  onUpdated,
}: {
  memory: MemoryDetail;
  canChangeAccessPolicy: boolean;
  onReload: () => Promise<void>;
  onUpdated: (memory: MemoryDetail) => void;
}) {
  const [title, setTitle] = useState(memory.title);
  const [summary, setSummary] = useState(memory.summary ?? "");
  const [visibility, setVisibility] = useState(memory.visibility);
  const [accessPassword, setAccessPassword] = useState("");
  const [themeConfig, setThemeConfig] = useState(
    JSON.stringify(memory.themeConfig, null, 2),
  );
  const [eventStartAt, setEventStartAt] = useState(
    toDateTimeLocal(memory.eventStartAt),
  );
  const [expiresAt, setExpiresAt] = useState(toDateTimeLocal(memory.expiresAt));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [conflict, setConflict] = useState(false);

  async function update(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setConflict(false);
    try {
      const parsedTheme = JSON.parse(themeConfig) as unknown;
      if (!isJsonObject(parsedTheme)) {
        throw new Error("Theme config phải là một JSON object.");
      }
      const response = await authenticatedFetch(`/api/memories/${memory.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title,
          summary,
          visibility,
          accessPassword: accessPassword || null,
          themeConfig: parsedTheme,
          eventStartAt: toInstant(eventStartAt),
          expiresAt: toInstant(expiresAt),
          version: memory.version,
        }),
      });
      if (!response.ok) {
        const problem = await readMemoryProblem(response);
        setConflict(problem.code === "MEMORY_VERSION_CONFLICT");
        throw new Error(problem.detail);
      }
      setAccessPassword("");
      onUpdated((await response.json()) as MemoryDetail);
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  async function reloadCurrent() {
    setBusy(true);
    setError("");
    try {
      await onReload();
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="memory-editor" onSubmit={update}>
      <h3>Cập nhật draft</h3>
      <p className="form-note">
        Settings được giữ nguyên. Dữ liệu trong form không bị xóa khi lưu lỗi.
        {!canChangeAccessPolicy
          ? " Chỉ owner được đổi visibility hoặc mật khẩu truy cập."
          : ""}
      </p>

      <label htmlFor="draft-title">Tiêu đề</label>
      <input
        id="draft-title"
        value={title}
        onChange={(event) => setTitle(event.target.value)}
        maxLength={255}
        required
      />

      <label htmlFor="draft-summary">Tóm tắt (plain text/Markdown)</label>
      <textarea
        id="draft-summary"
        value={summary}
        onChange={(event) => setSummary(event.target.value)}
        maxLength={1000}
        rows={5}
      />

      {canChangeAccessPolicy ? (
        <>
          <label htmlFor="draft-visibility">Visibility</label>
          <select
            id="draft-visibility"
            value={visibility}
            onChange={(event) => {
              const nextVisibility = event.target
                .value as MemoryDetail["visibility"];
              setVisibility(nextVisibility);
              if (nextVisibility !== "PASSWORD_PROTECTED") {
                setAccessPassword("");
              }
            }}
          >
            <option value="PRIVATE">PRIVATE</option>
            <option value="UNLISTED">UNLISTED</option>
            <option value="PUBLIC">PUBLIC</option>
            <option value="PASSWORD_PROTECTED">PASSWORD_PROTECTED</option>
          </select>

          {visibility === "PASSWORD_PROTECTED" ? (
            <>
              <label htmlFor="draft-access-password">Mật khẩu truy cập</label>
              <input
                id="draft-access-password"
                type="password"
                value={accessPassword}
                onChange={(event) => setAccessPassword(event.target.value)}
                autoComplete="new-password"
                maxLength={72}
                required={memory.visibility !== "PASSWORD_PROTECTED"}
              />
              <p className="form-note">
                {memory.visibility === "PASSWORD_PROTECTED"
                  ? "Để trống để giữ mật khẩu hiện tại. Nhập mật khẩu mới sẽ thu hồi các lượt truy cập đã cấp."
                  : "Mật khẩu là bắt buộc khi chuyển sang PASSWORD_PROTECTED."}
              </p>
            </>
          ) : null}
        </>
      ) : null}

      <label htmlFor="draft-event-start">Thời điểm sự kiện</label>
      <input
        id="draft-event-start"
        type="datetime-local"
        value={eventStartAt}
        onChange={(event) => setEventStartAt(event.target.value)}
      />

      <label htmlFor="draft-expires">Thời điểm hết hạn</label>
      <input
        id="draft-expires"
        type="datetime-local"
        value={expiresAt}
        onChange={(event) => setExpiresAt(event.target.value)}
      />

      <label htmlFor="draft-theme">Theme config (JSON)</label>
      <textarea
        id="draft-theme"
        className="json-editor"
        value={themeConfig}
        onChange={(event) => setThemeConfig(event.target.value)}
        rows={12}
        spellCheck={false}
        required
      />

      <button type="submit" disabled={busy}>
        {busy ? "Đang lưu…" : "Lưu draft"}
      </button>
      {error ? (
        <div className="form-note form-error" role="alert">
          <p>{error}</p>
          {conflict ? (
            <button type="button" disabled={busy} onClick={() => void reloadCurrent()}>
              Tải lại bản hiện hành
            </button>
          ) : null}
        </div>
      ) : null}
    </form>
  );
}

async function fetchCatalog(page: number, memoryType: MemoryType) {
  const query = new URLSearchParams({
    page: page.toString(),
    size: "12",
    status: "ACTIVE",
  });
  if (memoryType) {
    query.set("memoryType", memoryType);
  }
  const response = await authenticatedFetch(`/api/templates?${query.toString()}`, {
    cache: "no-store",
  });
  if (!response.ok) {
    if (response.status === 401) {
      throw new Error("Vui lòng đăng nhập để xem catalog template.");
    }
    throw new Error("Chưa thể tải catalog template.");
  }
  return (await response.json()) as TemplateCatalogPage;
}

function firstSelection(catalog: TemplateCatalogPage): SelectedTemplate | null {
  const template = catalog.items[0];
  const version = template?.versions[0];
  return template && version ? { template, version } : null;
}

function catalogPreviewPayload(selected: SelectedTemplate): MemoryRenderPayload {
  const configuredTitle = selected.version.defaultConfig.title;
  return {
    slug: "template-preview",
    title:
      typeof configuredTitle === "string" && configuredTitle.trim()
        ? configuredTitle
        : selected.template.name,
    memoryType: selected.template.memoryType,
    status: "DRAFT",
    visibility: "PRIVATE",
    summary: null,
    themeConfig: selected.version.defaultConfig,
    eventStartAt: null,
    publishedAt: null,
    expiresAt: null,
    templateVersionId: selected.version.id,
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

async function readMemoryProblem(response: Response): Promise<MemoryProblem> {
  try {
    const problem = (await response.json()) as Omit<MemoryProblem, "detail"> & {
      detail?: string;
    };
    return {
      code: problem.code,
      detail: problem.detail ?? "Không thể xử lý memory.",
    };
  } catch {
    return { detail: "Không thể xử lý memory." };
  }
}

function isJsonObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function toDateTimeLocal(value: string | null) {
  if (!value) {
    return "";
  }
  const date = new Date(value);
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return localDate.toISOString().slice(0, 16);
}

function toInstant(value: string) {
  return value ? new Date(value).toISOString() : null;
}

function errorMessage(reason: unknown) {
  return reason instanceof Error ? reason.message : "Dịch vụ tạm thời không khả dụng.";
}
