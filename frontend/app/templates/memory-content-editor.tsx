"use client";

import { type FormEvent, useCallback, useEffect, useState } from "react";

import { authenticatedFetch } from "@/lib/auth-session";

type MemoryMember = {
  id: string;
  roleCode: string;
  fullName: string;
  displayName: string | null;
  description: string | null;
  avatarAssetId: string | null;
  sortOrder: number;
  version: number;
};

type MemorySection = {
  id: string;
  sectionKey: string;
  sectionType: string;
  title: string | null;
  contentText: string | null;
  config: Record<string, unknown>;
  sortOrder: number;
  visible: boolean;
  required: boolean;
  contentComplete: boolean;
  version: number;
};

type VersionedItem = {
  id: string;
  version: number;
};

class ContentProblem extends Error {
  constructor(
    message: string,
    readonly code?: string,
  ) {
    super(message);
  }
}

export default function MemoryContentEditor({
  memoryId,
  allowedSectionTypes,
}: {
  memoryId: string;
  allowedSectionTypes: string[];
}) {
  const [members, setMembers] = useState<MemoryMember[]>([]);
  const [sections, setSections] = useState<MemorySection[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setBusy(true);
    setError("");
    try {
      const [loadedMembers, loadedSections] = await Promise.all([
        requestJson<MemoryMember[]>(`/api/memories/${memoryId}/members`),
        requestJson<MemorySection[]>(`/api/memories/${memoryId}/sections`),
      ]);
      setMembers(loadedMembers);
      setSections(loadedSections);
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }, [memoryId]);

  useEffect(() => {
    let active = true;
    void Promise.all([
      requestJson<MemoryMember[]>(`/api/memories/${memoryId}/members`),
      requestJson<MemorySection[]>(`/api/memories/${memoryId}/sections`),
    ])
      .then(([loadedMembers, loadedSections]) => {
        if (active) {
          setMembers(loadedMembers);
          setSections(loadedSections);
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
  }, [memoryId]);

  async function moveMember(index: number, direction: -1 | 1) {
    const reordered = move(members, index, direction);
    if (!reordered) {
      return;
    }
    setBusy(true);
    setError("");
    try {
      setMembers(
        await reorderItems<MemoryMember>(
          `/api/memories/${memoryId}/members/order`,
          reordered,
        ),
      );
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  async function moveSection(index: number, direction: -1 | 1) {
    const reordered = move(sections, index, direction);
    if (!reordered) {
      return;
    }
    setBusy(true);
    setError("");
    try {
      setSections(
        await reorderItems<MemorySection>(
          `/api/memories/${memoryId}/sections/order`,
          reordered,
        ),
      );
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="memory-content-editor" aria-busy={loading || busy}>
      <header>
        <div>
          <h3>Nhân vật và section</h3>
          <p className="form-note">
            Nội dung draft có thể chưa hoàn chỉnh; điều kiện bắt buộc sẽ được kiểm
            tra trước khi publish.
          </p>
        </div>
        <button type="button" disabled={loading || busy} onClick={() => void load()}>
          Tải lại nội dung
        </button>
      </header>

      {error ? (
        <p className="form-note form-error" role="alert">
          {error}
        </p>
      ) : null}
      {loading ? <p>Đang tải nội dung memory…</p> : null}

      {!loading ? (
        <div className="content-editor-columns">
          <section>
            <h4>Nhân vật</h4>
            <CreateMemberForm
              memoryId={memoryId}
              sortOrder={nextSortOrder(members)}
              onCreated={(member) => setMembers((current) => [...current, member])}
            />
            <div className="content-item-list">
              {members.map((member, index) => (
                <MemberCard
                  key={`${member.id}-${member.version}`}
                  memoryId={memoryId}
                  member={member}
                  first={index === 0}
                  last={index === members.length - 1}
                  onMove={(direction) => void moveMember(index, direction)}
                  onUpdated={(updated) =>
                    setMembers((current) => replaceItem(current, updated))
                  }
                  onDeleted={() =>
                    setMembers((current) =>
                      current.filter((item) => item.id !== member.id),
                    )
                  }
                  onReload={load}
                />
              ))}
            </div>
          </section>

          <section>
            <h4>Section</h4>
            <CreateSectionForm
              memoryId={memoryId}
              allowedSectionTypes={allowedSectionTypes}
              sortOrder={nextSortOrder(sections)}
              onCreated={(section) => setSections((current) => [...current, section])}
            />
            <div className="content-item-list">
              {sections.map((section, index) => (
                <SectionCard
                  key={`${section.id}-${section.version}`}
                  memoryId={memoryId}
                  section={section}
                  allowedSectionTypes={allowedSectionTypes}
                  first={index === 0}
                  last={index === sections.length - 1}
                  onMove={(direction) => void moveSection(index, direction)}
                  onUpdated={(updated) =>
                    setSections((current) => replaceItem(current, updated))
                  }
                  onDeleted={() =>
                    setSections((current) =>
                      current.filter((item) => item.id !== section.id),
                    )
                  }
                  onReload={load}
                />
              ))}
            </div>
          </section>
        </div>
      ) : null}
    </section>
  );
}

function CreateMemberForm({
  memoryId,
  sortOrder,
  onCreated,
}: {
  memoryId: string;
  sortOrder: number;
  onCreated: (member: MemoryMember) => void;
}) {
  const [roleCode, setRoleCode] = useState("OWNER");
  const [fullName, setFullName] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [description, setDescription] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const member = await requestJson<MemoryMember>(
        `/api/memories/${memoryId}/members`,
        "POST",
        { roleCode, fullName, displayName, description, sortOrder },
      );
      onCreated(member);
      setFullName("");
      setDisplayName("");
      setDescription("");
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="content-item-form" onSubmit={submit}>
      <label htmlFor="new-member-role">Role code</label>
      <input
        id="new-member-role"
        value={roleCode}
        onChange={(event) => setRoleCode(event.target.value.toUpperCase())}
        pattern="[A-Z][A-Z0-9_]{0,49}"
        maxLength={50}
        required
      />
      <label htmlFor="new-member-name">Tên đầy đủ</label>
      <input
        id="new-member-name"
        value={fullName}
        onChange={(event) => setFullName(event.target.value)}
        maxLength={200}
        required
      />
      <label htmlFor="new-member-display">Tên hiển thị</label>
      <input
        id="new-member-display"
        value={displayName}
        onChange={(event) => setDisplayName(event.target.value)}
        maxLength={150}
      />
      <label htmlFor="new-member-description">Mô tả (plain text/Markdown)</label>
      <textarea
        id="new-member-description"
        value={description}
        onChange={(event) => setDescription(event.target.value)}
        rows={4}
      />
      <button type="submit" disabled={busy}>
        {busy ? "Đang thêm…" : "Thêm nhân vật"}
      </button>
      <FormError error={error} />
    </form>
  );
}

function MemberCard({
  memoryId,
  member,
  first,
  last,
  onMove,
  onUpdated,
  onDeleted,
  onReload,
}: {
  memoryId: string;
  member: MemoryMember;
  first: boolean;
  last: boolean;
  onMove: (direction: -1 | 1) => void;
  onUpdated: (member: MemoryMember) => void;
  onDeleted: () => void;
  onReload: () => Promise<void>;
}) {
  const [roleCode, setRoleCode] = useState(member.roleCode);
  const [fullName, setFullName] = useState(member.fullName);
  const [displayName, setDisplayName] = useState(member.displayName ?? "");
  const [description, setDescription] = useState(member.description ?? "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [conflict, setConflict] = useState(false);

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setConflict(false);
    try {
      onUpdated(
        await requestJson<MemoryMember>(
          `/api/memories/${memoryId}/members/${member.id}`,
          "PUT",
          { roleCode, fullName, displayName, description, version: member.version },
        ),
      );
    } catch (reason) {
      setConflict(isConflict(reason));
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  async function remove() {
    if (!window.confirm(`Xóa nhân vật “${member.fullName}”?`)) {
      return;
    }
    setBusy(true);
    setError("");
    try {
      await requestJson<void>(
        `/api/memories/${memoryId}/members/${member.id}?version=${member.version}`,
        "DELETE",
      );
      onDeleted();
    } catch (reason) {
      setConflict(isConflict(reason));
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="content-item-card" onSubmit={save}>
      <div className="content-item-heading">
        <strong>{member.fullName}</strong>
        <span>#{member.sortOrder}</span>
      </div>
      <label htmlFor={`member-role-${member.id}`}>Role code</label>
      <input
        id={`member-role-${member.id}`}
        value={roleCode}
        onChange={(event) => setRoleCode(event.target.value.toUpperCase())}
        pattern="[A-Z][A-Z0-9_]{0,49}"
        maxLength={50}
        required
      />
      <label htmlFor={`member-name-${member.id}`}>Tên đầy đủ</label>
      <input
        id={`member-name-${member.id}`}
        value={fullName}
        onChange={(event) => setFullName(event.target.value)}
        maxLength={200}
        required
      />
      <label htmlFor={`member-display-${member.id}`}>Tên hiển thị</label>
      <input
        id={`member-display-${member.id}`}
        value={displayName}
        onChange={(event) => setDisplayName(event.target.value)}
        maxLength={150}
      />
      <label htmlFor={`member-description-${member.id}`}>Mô tả</label>
      <textarea
        id={`member-description-${member.id}`}
        value={description}
        onChange={(event) => setDescription(event.target.value)}
        rows={4}
      />
      <ItemActions
        busy={busy}
        first={first}
        last={last}
        onMove={onMove}
        onDelete={() => void remove()}
      />
      <FormError error={error} conflict={conflict} onReload={onReload} />
    </form>
  );
}

function CreateSectionForm({
  memoryId,
  allowedSectionTypes,
  sortOrder,
  onCreated,
}: {
  memoryId: string;
  allowedSectionTypes: string[];
  sortOrder: number;
  onCreated: (section: MemorySection) => void;
}) {
  const [sectionKey, setSectionKey] = useState("");
  const [sectionType, setSectionType] = useState(allowedSectionTypes[0] ?? "");
  const [title, setTitle] = useState("");
  const [contentText, setContentText] = useState("");
  const [config, setConfig] = useState("{}");
  const [visible, setVisible] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const parsedConfig = parseJsonObject(config);
      const section = await requestJson<MemorySection>(
        `/api/memories/${memoryId}/sections`,
        "POST",
        {
          sectionKey,
          sectionType,
          title,
          contentText,
          config: parsedConfig,
          sortOrder,
          visible,
        },
      );
      onCreated(section);
      setSectionKey("");
      setTitle("");
      setContentText("");
      setConfig("{}");
      setVisible(true);
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="content-item-form" onSubmit={submit}>
      {allowedSectionTypes.length === 0 ? (
        <p className="form-note form-error">
          Template version chưa khai báo section contract.
        </p>
      ) : null}
      <label htmlFor="new-section-key">Section key bất biến</label>
      <input
        id="new-section-key"
        value={sectionKey}
        onChange={(event) => setSectionKey(event.target.value)}
        maxLength={100}
        placeholder="hero-main"
        required
      />
      <label htmlFor="new-section-type">Section type</label>
      <select
        id="new-section-type"
        value={sectionType}
        onChange={(event) => setSectionType(event.target.value)}
        required
      >
        {allowedSectionTypes.map((type) => (
          <option key={type}>{type}</option>
        ))}
      </select>
      <label htmlFor="new-section-title">Tiêu đề</label>
      <input
        id="new-section-title"
        value={title}
        onChange={(event) => setTitle(event.target.value)}
        maxLength={255}
      />
      <label htmlFor="new-section-content">Nội dung (plain text/Markdown)</label>
      <textarea
        id="new-section-content"
        value={contentText}
        onChange={(event) => setContentText(event.target.value)}
        rows={5}
      />
      <label htmlFor="new-section-config">Config (JSON)</label>
      <textarea
        id="new-section-config"
        className="json-editor"
        value={config}
        onChange={(event) => setConfig(event.target.value)}
        rows={6}
        required
      />
      <label className="checkbox-label" htmlFor="new-section-visible">
        <input
          id="new-section-visible"
          type="checkbox"
          checked={visible}
          onChange={(event) => setVisible(event.target.checked)}
        />
        Hiển thị section
      </label>
      <button type="submit" disabled={busy || allowedSectionTypes.length === 0}>
        {busy ? "Đang thêm…" : "Thêm section"}
      </button>
      <FormError error={error} />
    </form>
  );
}

function SectionCard({
  memoryId,
  section,
  allowedSectionTypes,
  first,
  last,
  onMove,
  onUpdated,
  onDeleted,
  onReload,
}: {
  memoryId: string;
  section: MemorySection;
  allowedSectionTypes: string[];
  first: boolean;
  last: boolean;
  onMove: (direction: -1 | 1) => void;
  onUpdated: (section: MemorySection) => void;
  onDeleted: () => void;
  onReload: () => Promise<void>;
}) {
  const [sectionType, setSectionType] = useState(section.sectionType);
  const [title, setTitle] = useState(section.title ?? "");
  const [contentText, setContentText] = useState(section.contentText ?? "");
  const [config, setConfig] = useState(JSON.stringify(section.config, null, 2));
  const [visible, setVisible] = useState(section.visible);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [conflict, setConflict] = useState(false);

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setConflict(false);
    try {
      onUpdated(
        await requestJson<MemorySection>(
          `/api/memories/${memoryId}/sections/${section.id}`,
          "PUT",
          {
            sectionType,
            title,
            contentText,
            config: parseJsonObject(config),
            visible,
            version: section.version,
          },
        ),
      );
    } catch (reason) {
      setConflict(isConflict(reason));
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  async function remove() {
    if (!window.confirm(`Xóa section “${section.sectionKey}”?`)) {
      return;
    }
    setBusy(true);
    setError("");
    try {
      await requestJson<void>(
        `/api/memories/${memoryId}/sections/${section.id}?version=${section.version}`,
        "DELETE",
      );
      onDeleted();
    } catch (reason) {
      setConflict(isConflict(reason));
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="content-item-card" onSubmit={save}>
      <div className="content-item-heading">
        <strong>{section.sectionKey}</strong>
        <span>
          #{section.sortOrder} · {section.required ? "Bắt buộc" : "Tùy chọn"} ·{" "}
          {section.contentComplete ? "Có nội dung" : "Chưa hoàn chỉnh"}
        </span>
      </div>
      <label htmlFor={`section-type-${section.id}`}>Section type</label>
      <select
        id={`section-type-${section.id}`}
        value={sectionType}
        onChange={(event) => setSectionType(event.target.value)}
      >
        {allowedSectionTypes.map((type) => (
          <option key={type}>{type}</option>
        ))}
      </select>
      <label htmlFor={`section-title-${section.id}`}>Tiêu đề</label>
      <input
        id={`section-title-${section.id}`}
        value={title}
        onChange={(event) => setTitle(event.target.value)}
        maxLength={255}
      />
      <label htmlFor={`section-content-${section.id}`}>Nội dung</label>
      <textarea
        id={`section-content-${section.id}`}
        value={contentText}
        onChange={(event) => setContentText(event.target.value)}
        rows={5}
      />
      <label htmlFor={`section-config-${section.id}`}>Config (JSON)</label>
      <textarea
        id={`section-config-${section.id}`}
        className="json-editor"
        value={config}
        onChange={(event) => setConfig(event.target.value)}
        rows={6}
        required
      />
      <label className="checkbox-label" htmlFor={`section-visible-${section.id}`}>
        <input
          id={`section-visible-${section.id}`}
          type="checkbox"
          checked={visible}
          onChange={(event) => setVisible(event.target.checked)}
        />
        Hiển thị section
      </label>
      <ItemActions
        busy={busy}
        first={first}
        last={last}
        onMove={onMove}
        onDelete={() => void remove()}
      />
      <FormError error={error} conflict={conflict} onReload={onReload} />
    </form>
  );
}

function ItemActions({
  busy,
  first,
  last,
  onMove,
  onDelete,
}: {
  busy: boolean;
  first: boolean;
  last: boolean;
  onMove: (direction: -1 | 1) => void;
  onDelete: () => void;
}) {
  return (
    <div className="content-item-actions">
      <button type="submit" disabled={busy}>
        {busy ? "Đang xử lý…" : "Lưu"}
      </button>
      <button type="button" disabled={busy || first} onClick={() => onMove(-1)}>
        Lên
      </button>
      <button type="button" disabled={busy || last} onClick={() => onMove(1)}>
        Xuống
      </button>
      <button className="danger-button" type="button" disabled={busy} onClick={onDelete}>
        Xóa
      </button>
    </div>
  );
}

function FormError({
  error,
  conflict = false,
  onReload,
}: {
  error: string;
  conflict?: boolean;
  onReload?: () => Promise<void>;
}) {
  if (!error) {
    return null;
  }
  return (
    <div className="form-note form-error" role="alert">
      <p>{error}</p>
      {conflict && onReload ? (
        <button type="button" onClick={() => void onReload()}>
          Tải lại bản hiện hành
        </button>
      ) : null}
    </div>
  );
}

async function requestJson<T>(
  path: string,
  method: "GET" | "POST" | "PUT" | "DELETE" = "GET",
  body?: unknown,
): Promise<T> {
  const response = await authenticatedFetch(path, {
    method,
    ...(body === undefined
      ? {}
      : {
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
        }),
    cache: "no-store",
  });
  if (!response.ok) {
    const problem = await readProblem(response);
    throw new ContentProblem(problem.detail, problem.code);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

async function reorderItems<T extends VersionedItem>(path: string, items: T[]) {
  return requestJson<T[]>(path, "PUT", {
    orderedIds: items.map((item) => item.id),
    versions: Object.fromEntries(items.map((item) => [item.id, item.version])),
  });
}

async function readProblem(response: Response) {
  try {
    const problem = (await response.json()) as { detail?: string; code?: string };
    return {
      code: problem.code,
      detail: problem.detail ?? "Không thể cập nhật nội dung memory.",
    };
  } catch {
    return { detail: "Không thể cập nhật nội dung memory." };
  }
}

function move<T>(items: T[], index: number, direction: -1 | 1) {
  const target = index + direction;
  if (target < 0 || target >= items.length) {
    return null;
  }
  const reordered = [...items];
  [reordered[index], reordered[target]] = [reordered[target], reordered[index]];
  return reordered;
}

function replaceItem<T extends { id: string }>(items: T[], updated: T) {
  return items.map((item) => (item.id === updated.id ? updated : item));
}

function nextSortOrder(items: Array<{ sortOrder: number }>) {
  return items.reduce((maximum, item) => Math.max(maximum, item.sortOrder), -1) + 1;
}

function parseJsonObject(value: string): Record<string, unknown> {
  const parsed = JSON.parse(value) as unknown;
  if (typeof parsed !== "object" || parsed === null || Array.isArray(parsed)) {
    throw new Error("Config phải là một JSON object.");
  }
  return parsed as Record<string, unknown>;
}

function isConflict(reason: unknown) {
  return reason instanceof ContentProblem && reason.code === "MEMORY_VERSION_CONFLICT";
}

function errorMessage(reason: unknown) {
  return reason instanceof Error ? reason.message : "Dịch vụ tạm thời không khả dụng.";
}
