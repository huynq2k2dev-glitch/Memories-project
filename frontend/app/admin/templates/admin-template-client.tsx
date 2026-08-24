"use client";

import Link from "next/link";
import { type FormEvent, useCallback, useEffect, useState } from "react";

import { authenticatedFetch } from "@/lib/auth-session";

const MEMORY_TYPES = [
  "WEDDING",
  "FUNERAL",
  "GRADUATION",
  "HOUSEWARMING",
  "PERSONAL",
] as const;
const TEMPLATE_STATUSES = ["DRAFT", "ACTIVE", "INACTIVE", "ARCHIVED"] as const;
const BASIC_SCHEMA = JSON.stringify(
  {
    $schema: "https://json-schema.org/draft/2020-12/schema",
    type: "object",
    properties: {
      title: { type: "string" },
      subtitle: { type: "string" },
      accentColor: { type: "string" },
    },
    additionalProperties: false,
  },
  null,
  2,
);
const BASIC_SECTION_CONTRACTS = JSON.stringify(
  {
    HERO: {
      configSchema: {
        $schema: "https://json-schema.org/draft/2020-12/schema",
        type: "object",
        additionalProperties: false,
      },
    },
  },
  null,
  2,
);

type MemoryType = (typeof MEMORY_TYPES)[number];
type TemplateStatus = (typeof TEMPLATE_STATUSES)[number];
type TemplateVersionStatus = "DRAFT" | "PUBLISHED" | "DEPRECATED";

type AdminTemplateVersion = {
  id: string;
  versionNo: number;
  componentKey: string;
  rendererVersion: string;
  coverRequired: boolean;
  configSchema: Record<string, unknown>;
  defaultConfig: Record<string, unknown>;
  sectionContracts: Record<string, { configSchema: Record<string, unknown> }>;
  requiredSections: string[];
  status: TemplateVersionStatus;
  publishedAt: string | null;
};

type AdminTemplate = {
  id: string;
  code: string;
  name: string;
  memoryType: MemoryType;
  description: string | null;
  status: TemplateStatus;
  version: number;
  versions: AdminTemplateVersion[];
};

type AdminTemplatePage = {
  items: AdminTemplate[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};

type Mutation = (
  path: string,
  method: "POST" | "PUT",
  body: unknown,
  successMessage: string,
) => Promise<boolean>;

export default function AdminTemplateClient() {
  const [templatePage, setTemplatePage] = useState<AdminTemplatePage | null>(null);
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");

  const loadTemplates = useCallback(async (page = 0) => {
    setTemplatePage(await fetchTemplates(page));
  }, []);

  useEffect(() => {
    let active = true;
    void fetchTemplates(0)
      .then((loadedTemplates) => {
        if (active) {
          setTemplatePage(loadedTemplates);
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

  const templates = templatePage?.items ?? [];
  const currentPage = templatePage?.page ?? 0;

  async function changePage(page: number) {
    setLoading(true);
    setError("");
    try {
      await loadTemplates(page);
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setLoading(false);
    }
  }

  const mutate = useCallback<Mutation>(
    async (path, method, body, successMessage) => {
      setError("");
      setNotice("");
      try {
        const response = await authenticatedFetch(path, {
          method,
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
        });
        if (!response.ok) {
          throw new Error(await problemDetail(response));
        }
        await loadTemplates(currentPage);
        setNotice(successMessage);
        return true;
      } catch (reason) {
        setError(errorMessage(reason));
        return false;
      }
    },
    [currentPage, loadTemplates],
  );

  return (
    <main className="admin-shell">
      <section className="admin-header" aria-labelledby="template-admin-title">
        <div>
          <p className="eyebrow">Quản trị</p>
          <h1 id="template-admin-title">Template</h1>
          <p className="summary">
            Quản lý metadata và hợp đồng render. Chỉ component có trong frontend
            build mới được publish.
          </p>
        </div>
        <Link className="secondary-link" href="/login">
          Đăng nhập
        </Link>
      </section>

      {notice ? <p className="admin-notice">{notice}</p> : null}
      {error ? (
        <p className="form-note form-error" role="alert">
          {error}
        </p>
      ) : null}

      <CreateTemplateForm mutate={mutate} />

      <section className="template-list" aria-busy={loading}>
        {loading ? <p>Đang tải template…</p> : null}
        {!loading && templates.length === 0 ? (
          <p>Chưa có template nào.</p>
        ) : null}
        {templates.map((template) => (
          <TemplateCard key={template.id} template={template} mutate={mutate} />
        ))}
        {templatePage && templatePage.totalPages > 1 ? (
          <nav className="pagination" aria-label="Phân trang template quản trị">
            <button
              type="button"
              disabled={loading || templatePage.page === 0}
              onClick={() => void changePage(templatePage.page - 1)}
            >
              Trang trước
            </button>
            <span>
              Trang {templatePage.page + 1}/{templatePage.totalPages}
            </span>
            <button
              type="button"
              disabled={loading || templatePage.page + 1 >= templatePage.totalPages}
              onClick={() => void changePage(templatePage.page + 1)}
            >
              Trang sau
            </button>
          </nav>
        ) : null}
      </section>
    </main>
  );
}

function CreateTemplateForm({ mutate }: { mutate: Mutation }) {
  const [code, setCode] = useState("");
  const [name, setName] = useState("");
  const [memoryType, setMemoryType] = useState<MemoryType>("PERSONAL");
  const [description, setDescription] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    const created = await mutate(
      "/api/admin/templates",
      "POST",
      { code, name, memoryType, description },
      "Đã tạo template draft.",
    );
    if (created) {
      setCode("");
      setName("");
      setDescription("");
    }
    setBusy(false);
  }

  return (
    <form className="admin-form" onSubmit={submit}>
      <h2>Tạo template</h2>
      <label htmlFor="template-code">Code bất biến</label>
      <input
        id="template-code"
        value={code}
        onChange={(event) => setCode(event.target.value.toUpperCase())}
        placeholder="PERSONAL_BASIC"
        pattern="[A-Z][A-Z0-9_]{2,99}"
        maxLength={100}
        required
      />
      <label htmlFor="template-name">Tên</label>
      <input
        id="template-name"
        value={name}
        onChange={(event) => setName(event.target.value)}
        maxLength={150}
        required
      />
      <label htmlFor="template-memory-type">Loại memory</label>
      <select
        id="template-memory-type"
        value={memoryType}
        onChange={(event) => setMemoryType(event.target.value as MemoryType)}
      >
        {MEMORY_TYPES.map((value) => (
          <option key={value}>{value}</option>
        ))}
      </select>
      <label htmlFor="template-description">Mô tả</label>
      <textarea
        id="template-description"
        value={description}
        onChange={(event) => setDescription(event.target.value)}
        maxLength={1000}
      />
      <button type="submit" disabled={busy}>
        {busy ? "Đang tạo…" : "Tạo template"}
      </button>
    </form>
  );
}

function TemplateCard({ template, mutate }: { template: AdminTemplate; mutate: Mutation }) {
  const [name, setName] = useState(template.name);
  const [memoryType, setMemoryType] = useState<MemoryType>(template.memoryType);
  const [description, setDescription] = useState(template.description ?? "");
  const [status, setStatus] = useState<TemplateStatus>(template.status);
  const [busy, setBusy] = useState(false);

  async function updateMetadata(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    await mutate(
      `/api/admin/templates/${template.id}`,
      "PUT",
      { name, memoryType, description, status },
      `Đã cập nhật ${template.code}.`,
    );
    setBusy(false);
  }

  return (
    <article className="template-card">
      <header className="template-card-header">
        <div>
          <p className="template-code">{template.code}</p>
          <h2>{template.name}</h2>
        </div>
        <span className="status-badge">{template.status}</span>
      </header>

      <form className="admin-form compact" onSubmit={updateMetadata}>
        <label htmlFor={`name-${template.id}`}>Tên</label>
        <input
          id={`name-${template.id}`}
          value={name}
          onChange={(event) => setName(event.target.value)}
          maxLength={150}
          required
        />
        <label htmlFor={`type-${template.id}`}>Loại memory</label>
        <select
          id={`type-${template.id}`}
          value={memoryType}
          onChange={(event) => setMemoryType(event.target.value as MemoryType)}
        >
          {MEMORY_TYPES.map((value) => (
            <option key={value}>{value}</option>
          ))}
        </select>
        <label htmlFor={`status-${template.id}`}>Trạng thái</label>
        <select
          id={`status-${template.id}`}
          value={status}
          onChange={(event) => setStatus(event.target.value as TemplateStatus)}
        >
          {TEMPLATE_STATUSES.map((value) => (
            <option key={value}>{value}</option>
          ))}
        </select>
        <label htmlFor={`description-${template.id}`}>Mô tả</label>
        <textarea
          id={`description-${template.id}`}
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          maxLength={1000}
        />
        <button type="submit" disabled={busy}>
          {busy ? "Đang lưu…" : "Lưu metadata"}
        </button>
      </form>

      <section className="version-section">
        <h3>Phiên bản render</h3>
        <VersionForm templateId={template.id} mutate={mutate} />
        {template.versions.map((version) => (
          <VersionCard
            key={version.id}
            templateId={template.id}
            version={version}
            mutate={mutate}
          />
        ))}
      </section>
    </article>
  );
}

function VersionCard({
  templateId,
  version,
  mutate,
}: {
  templateId: string;
  version: AdminTemplateVersion;
  mutate: Mutation;
}) {
  const [busy, setBusy] = useState(false);

  async function transition(action: "publish" | "deprecate") {
    setBusy(true);
    await mutate(
      `/api/admin/templates/${templateId}/versions/${version.id}/${action}`,
      "POST",
      {},
      action === "publish" ? "Đã publish phiên bản." : "Đã deprecate phiên bản.",
    );
    setBusy(false);
  }

  return (
    <article className="version-card">
      <header className="version-card-header">
        <strong>Version {version.versionNo}</strong>
        <span className="status-badge">{version.status}</span>
      </header>
      {version.status === "DRAFT" ? (
        <>
          <VersionForm templateId={templateId} version={version} mutate={mutate} />
          <button
            className="publish-button"
            type="button"
            disabled={busy}
            onClick={() => void transition("publish")}
          >
            {busy ? "Đang publish…" : "Publish version"}
          </button>
        </>
      ) : (
        <dl className="contract-summary">
          <div>
            <dt>Component</dt>
            <dd>{version.componentKey}</dd>
          </div>
          <div>
            <dt>Renderer</dt>
            <dd>{version.rendererVersion}</dd>
          </div>
          <div>
            <dt>Cover bắt buộc</dt>
            <dd>{version.coverRequired ? "Có" : "Không"}</dd>
          </div>
          <div>
            <dt>Required sections</dt>
            <dd>{version.requiredSections.join(", ") || "Không có"}</dd>
          </div>
          <div>
            <dt>Allowed section types</dt>
            <dd>{Object.keys(version.sectionContracts).join(", ") || "Không có"}</dd>
          </div>
        </dl>
      )}
      {version.status === "PUBLISHED" ? (
        <button
          className="secondary-button"
          type="button"
          disabled={busy}
          onClick={() => void transition("deprecate")}
        >
          {busy ? "Đang cập nhật…" : "Deprecate version"}
        </button>
      ) : null}
    </article>
  );
}

function VersionForm({
  templateId,
  version,
  mutate,
}: {
  templateId: string;
  version?: AdminTemplateVersion;
  mutate: Mutation;
}) {
  const [configSchema, setConfigSchema] = useState(
    version ? JSON.stringify(version.configSchema, null, 2) : BASIC_SCHEMA,
  );
  const [defaultConfig, setDefaultConfig] = useState(
    JSON.stringify(version?.defaultConfig ?? {}, null, 2),
  );
  const [sectionContracts, setSectionContracts] = useState(
    version
      ? JSON.stringify(version.sectionContracts, null, 2)
      : BASIC_SECTION_CONTRACTS,
  );
  const [requiredSections, setRequiredSections] = useState(
    version?.requiredSections.join(", ") ?? "HERO",
  );
  const [coverRequired, setCoverRequired] = useState(version?.coverRequired ?? false);
  const [localError, setLocalError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLocalError("");
    let parsedSchema: unknown;
    let parsedDefault: unknown;
    let parsedSectionContracts: unknown;
    try {
      parsedSchema = JSON.parse(configSchema);
      parsedDefault = JSON.parse(defaultConfig);
      parsedSectionContracts = JSON.parse(sectionContracts);
    } catch {
      setLocalError(
        "Config schema, default config và section contracts phải là JSON hợp lệ.",
      );
      return;
    }

    setBusy(true);
    const path = version
      ? `/api/admin/templates/${templateId}/versions/${version.id}`
      : `/api/admin/templates/${templateId}/versions`;
    const saved = await mutate(
      path,
      version ? "PUT" : "POST",
      {
        componentKey: "memories-basic-v1",
        rendererVersion: "1",
        coverRequired,
        configSchema: parsedSchema,
        defaultConfig: parsedDefault,
        sectionContracts: parsedSectionContracts,
        requiredSections: requiredSections
          .split(",")
          .map((value) => value.trim().toUpperCase())
          .filter(Boolean),
      },
      version ? "Đã lưu version draft." : "Đã tạo version draft.",
    );
    if (saved && !version) {
      setConfigSchema(BASIC_SCHEMA);
      setDefaultConfig("{}");
      setSectionContracts(BASIC_SECTION_CONTRACTS);
      setRequiredSections("HERO");
      setCoverRequired(false);
    }
    setBusy(false);
  }

  return (
    <form className="version-form" onSubmit={submit}>
      <p className="renderer-contract">
        Renderer khả dụng: <code>memories-basic-v1@1</code>
      </p>
      <label className="checkbox-label">
        <input
          type="checkbox"
          checked={coverRequired}
          onChange={(event) => setCoverRequired(event.target.checked)}
        />
        Yêu cầu ảnh cover trước khi publish memory
      </label>
      <label htmlFor={`schema-${version?.id ?? templateId}`}>Config schema</label>
      <textarea
        id={`schema-${version?.id ?? templateId}`}
        className="json-field"
        value={configSchema}
        onChange={(event) => setConfigSchema(event.target.value)}
        required
      />
      <label htmlFor={`default-${version?.id ?? templateId}`}>Default config</label>
      <textarea
        id={`default-${version?.id ?? templateId}`}
        className="json-field"
        value={defaultConfig}
        onChange={(event) => setDefaultConfig(event.target.value)}
        required
      />
      <label htmlFor={`section-contracts-${version?.id ?? templateId}`}>
        Section contracts
      </label>
      <textarea
        id={`section-contracts-${version?.id ?? templateId}`}
        className="json-field"
        value={sectionContracts}
        onChange={(event) => setSectionContracts(event.target.value)}
        required
      />
      <label htmlFor={`sections-${version?.id ?? templateId}`}>
        Required sections, phân cách bằng dấu phẩy
      </label>
      <input
        id={`sections-${version?.id ?? templateId}`}
        value={requiredSections}
        onChange={(event) => setRequiredSections(event.target.value)}
        placeholder="HERO, GALLERY"
      />
      {localError ? (
        <p className="form-note form-error" role="alert">
          {localError}
        </p>
      ) : null}
      <button type="submit" disabled={busy}>
        {busy ? "Đang lưu…" : version ? "Lưu draft" : "Tạo version draft"}
      </button>
    </form>
  );
}

async function problemDetail(response: Response) {
  try {
    const problem = (await response.json()) as { detail?: string; code?: string };
    if (response.status === 403) {
      return "Tài khoản hiện tại không có quyền TEMPLATE_MANAGE.";
    }
    return problem.detail ?? problem.code ?? "Yêu cầu không thành công.";
  } catch {
    return "Yêu cầu không thành công.";
  }
}

async function fetchTemplates(page: number) {
  const query = new URLSearchParams({ page: String(page), size: "20" });
  const response = await authenticatedFetch(`/api/admin/templates?${query}`, {
    cache: "no-store",
  });
  if (!response.ok) {
    throw new Error(await problemDetail(response));
  }
  return (await response.json()) as AdminTemplatePage;
}

function errorMessage(reason: unknown) {
  return reason instanceof Error ? reason.message : "Dịch vụ tạm thời không khả dụng.";
}
