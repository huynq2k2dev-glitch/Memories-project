"use client";

import { type FormEvent, useCallback, useEffect, useState } from "react";

import { authenticatedFetch } from "@/lib/auth-session";

const ALLOWED_IMAGE_TYPES = [
  "image/jpeg",
  "image/png",
  "image/webp",
  "image/avif",
];
const MAX_FILE_SIZE = 10 * 1024 * 1024;

type MemoryImage = {
  id: string;
  assetId: string;
  sectionId: string | null;
  caption: string | null;
  altText: string | null;
  sortOrder: number;
  coverCandidate: boolean;
  deliveryUrl: string;
  assetVersion: number;
  version: number;
};

type MemorySection = {
  id: string;
  sectionKey: string;
  title: string | null;
};

type MemoryMember = {
  id: string;
  fullName: string;
  avatarAssetId: string | null;
  version: number;
};

type UploadTarget = {
  assetId: string;
  uploadUrl: string;
  method: "PUT";
  requiredHeaders: Record<string, string>;
  expiresAt: string;
};

type CoverResult = {
  coverAssetId: string | null;
  version: number;
};

class MediaProblem extends Error {
  constructor(
    message: string,
    readonly code?: string,
  ) {
    super(message);
  }
}

export default function MemoryMediaEditor({
  memoryId,
  memoryVersion,
  coverAssetId,
  onCoverUpdated,
}: {
  memoryId: string;
  memoryVersion: number;
  coverAssetId: string | null;
  onCoverUpdated: (result: CoverResult) => void;
}) {
  const [images, setImages] = useState<MemoryImage[]>([]);
  const [sections, setSections] = useState<MemorySection[]>([]);
  const [members, setMembers] = useState<MemoryMember[]>([]);
  const [file, setFile] = useState<File | null>(null);
  const [sectionId, setSectionId] = useState("");
  const [caption, setCaption] = useState("");
  const [altText, setAltText] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setBusy(true);
    setError("");
    try {
      const [loadedImages, loadedSections, loadedMembers] = await Promise.all([
        requestJson<MemoryImage[]>(`/api/memories/${memoryId}/images`),
        requestJson<MemorySection[]>(`/api/memories/${memoryId}/sections`),
        requestJson<MemoryMember[]>(`/api/memories/${memoryId}/members`),
      ]);
      setImages(loadedImages);
      setSections(loadedSections);
      setMembers(loadedMembers);
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }, [memoryId]);

  useEffect(() => {
    let active = true;
    void Promise.all([
      requestJson<MemoryImage[]>(`/api/memories/${memoryId}/images`),
      requestJson<MemorySection[]>(`/api/memories/${memoryId}/sections`),
      requestJson<MemoryMember[]>(`/api/memories/${memoryId}/members`),
    ])
      .then(([loadedImages, loadedSections, loadedMembers]) => {
        if (active) {
          setImages(loadedImages);
          setSections(loadedSections);
          setMembers(loadedMembers);
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

  async function upload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    if (!file) {
      setError("Hãy chọn một file ảnh.");
      return;
    }
    setBusy(true);
    setError("");
    try {
      validateFile(file);
      const checksumSha256 = await sha256Base64(file);
      const target = await requestJson<UploadTarget>(
        `/api/memories/${memoryId}/media/uploads`,
        "POST",
        {
          originalFileName: file.name,
          mimeType: file.type,
          fileSize: file.size,
          checksumSha256,
        },
      );
      const storageResponse = await fetch(target.uploadUrl, {
        method: target.method,
        headers: target.requiredHeaders,
        body: file,
      });
      if (!storageResponse.ok) {
        throw new Error("Object storage từ chối file upload.");
      }
      await requestJson(`/api/media/${target.assetId}/complete`, "POST");
      const image = await requestJson<MemoryImage>(
        `/api/memories/${memoryId}/images`,
        "POST",
        {
          assetId: target.assetId,
          sectionId: sectionId || null,
          caption,
          altText,
          sortOrder: nextSortOrder(images),
          coverCandidate: false,
        },
      );
      setImages((current) => [...current, image]);
      setFile(null);
      setCaption("");
      setAltText("");
      setSectionId("");
      form.reset();
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  async function moveImage(index: number, direction: -1 | 1) {
    const reordered = move(images, index, direction);
    if (!reordered) {
      return;
    }
    setBusy(true);
    setError("");
    try {
      setImages(
        await requestJson<MemoryImage[]>(
          `/api/memories/${memoryId}/images/order`,
          "PUT",
          {
            orderedIds: reordered.map((image) => image.id),
            versions: Object.fromEntries(
              reordered.map((image) => [image.id, image.version]),
            ),
          },
        ),
      );
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  async function clearCover() {
    setBusy(true);
    setError("");
    try {
      onCoverUpdated(
        await requestJson<CoverResult>(
          `/api/memories/${memoryId}/cover`,
          "PUT",
          { assetId: null, version: memoryVersion },
        ),
      );
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="memory-media-editor" aria-busy={loading || busy}>
      <header>
        <div>
          <h3>Album ảnh của bạn</h3>
          <p className="form-note">
            Chọn ảnh JPEG, PNG, WebP hoặc AVIF, tối đa 10 MiB mỗi ảnh. Bạn có thể thêm chú thích và sắp xếp sau.
          </p>
        </div>
        <div className="content-item-actions">
          <button type="button" disabled={loading || busy} onClick={() => void load()}>
            Tải lại ảnh
          </button>
          {coverAssetId ? (
            <button type="button" disabled={busy} onClick={() => void clearCover()}>
              Bỏ ảnh bìa
            </button>
          ) : null}
        </div>
      </header>

      {error ? (
        <p className="form-note form-error" role="alert">
          {error}
        </p>
      ) : null}

      <form className="content-item-form" onSubmit={upload}>
        <label htmlFor="memory-image-file">Chọn ảnh từ thiết bị</label>
        <input
          id="memory-image-file"
          type="file"
          accept={ALLOWED_IMAGE_TYPES.join(",")}
          onChange={(event) => setFile(event.target.files?.[0] ?? null)}
          required
        />
        <details className="creator-extra"><summary>Vị trí ảnh và mô tả hỗ trợ đọc</summary>
        <label htmlFor="new-image-section">Đặt ảnh vào phần</label>
        <select
          id="new-image-section"
          value={sectionId}
          onChange={(event) => setSectionId(event.target.value)}
        >
          <option value="">Album chung</option>
          {sections.map((section, index) => (
            <option key={section.id} value={section.id}>
              {section.title || `Phần nội dung ${index + 1}`}
            </option>
          ))}
        </select>
        <label htmlFor="new-image-caption">Chú thích ảnh</label>
        <textarea
          id="new-image-caption"
          value={caption}
          onChange={(event) => setCaption(event.target.value)}
          maxLength={1000}
          rows={3}
        />
        <label htmlFor="new-image-alt">Mô tả ảnh cho người dùng trình đọc màn hình</label>
        <input
          id="new-image-alt"
          value={altText}
          onChange={(event) => setAltText(event.target.value)}
          maxLength={500}
        />
        </details>
        <button type="submit" disabled={busy || loading}>
          {busy ? "Đang tải ảnh lên…" : "Thêm ảnh vào kỷ niệm"}
        </button>
      </form>

      {!loading ? (
        <div className="media-image-list">
          {images.length === 0 ? <p>Album còn trống. Bắt đầu bằng một bức ảnh bạn yêu thích.</p> : null}
          {images.map((image, index) => (
            <MemoryImageCard
              key={`${image.id}-${image.version}`}
              memoryId={memoryId}
              memoryVersion={memoryVersion}
              coverAssetId={coverAssetId}
              image={image}
              sections={sections}
              first={index === 0}
              last={index === images.length - 1}
              onMove={(direction) => void moveImage(index, direction)}
              onUpdated={(updated) =>
                setImages((current) => replaceItem(current, updated))
              }
              onDeleted={() =>
                setImages((current) => current.filter((item) => item.id !== image.id))
              }
              onCoverUpdated={onCoverUpdated}
            />
          ))}
        </div>
      ) : null}

      {!loading && members.length > 0 ? (
        <section className="avatar-assignment">
          <h4>Ảnh đại diện</h4>
          <p className="form-note">Chọn một ảnh đã tải lên trong album.</p>
          {members.map((member) => (
            <MemberAvatarEditor
              key={`${member.id}-${member.version}`}
              memoryId={memoryId}
              member={member}
              images={images}
              onUpdated={(updated) =>
                setMembers((current) => replaceItem(current, updated))
              }
            />
          ))}
        </section>
      ) : null}
    </section>
  );
}

function MemoryImageCard({
  memoryId,
  memoryVersion,
  coverAssetId,
  image,
  sections,
  first,
  last,
  onMove,
  onUpdated,
  onDeleted,
  onCoverUpdated,
}: {
  memoryId: string;
  memoryVersion: number;
  coverAssetId: string | null;
  image: MemoryImage;
  sections: MemorySection[];
  first: boolean;
  last: boolean;
  onMove: (direction: -1 | 1) => void;
  onUpdated: (image: MemoryImage) => void;
  onDeleted: () => void;
  onCoverUpdated: (result: CoverResult) => void;
}) {
  const [sectionId, setSectionId] = useState(image.sectionId ?? "");
  const [caption, setCaption] = useState(image.caption ?? "");
  const [altText, setAltText] = useState(image.altText ?? "");
  const [coverCandidate, setCoverCandidate] = useState(image.coverCandidate);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      onUpdated(
        await requestJson<MemoryImage>(
          `/api/memories/${memoryId}/images/${image.id}`,
          "PUT",
          {
            sectionId: sectionId || null,
            caption,
            altText,
            coverCandidate,
            version: image.version,
          },
        ),
      );
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  async function setCover() {
    setBusy(true);
    setError("");
    try {
      onCoverUpdated(
        await requestJson<CoverResult>(
          `/api/memories/${memoryId}/cover`,
          "PUT",
          { assetId: image.assetId, version: memoryVersion },
        ),
      );
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  async function unlink() {
    if (!window.confirm("Gỡ ảnh này khỏi kỷ niệm?")) {
      return;
    }
    setBusy(true);
    setError("");
    try {
      await requestJson<void>(
        `/api/memories/${memoryId}/images/${image.id}?version=${image.version}`,
        "DELETE",
      );
      onDeleted();
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  return (
    <article className="media-image-card">
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img src={image.deliveryUrl} alt={image.altText || "Ảnh memory"} />
      <form className="content-item-card" onSubmit={save}>
        <div className="content-item-heading">
          <strong>{coverAssetId === image.assetId ? "Ảnh bìa hiện tại" : "Ảnh trong album"}</strong>
        </div>
        <label htmlFor={`image-section-${image.id}`}>Đặt ảnh vào phần</label>
        <select
          id={`image-section-${image.id}`}
          value={sectionId}
          onChange={(event) => setSectionId(event.target.value)}
        >
          <option value="">Album chung</option>
          {sections.map((section, index) => (
            <option key={section.id} value={section.id}>
              {section.title || `Phần nội dung ${index + 1}`}
            </option>
          ))}
        </select>
        <label htmlFor={`image-caption-${image.id}`}>Chú thích</label>
        <textarea
          id={`image-caption-${image.id}`}
          value={caption}
          onChange={(event) => setCaption(event.target.value)}
          maxLength={1000}
          rows={3}
        />
        <label htmlFor={`image-alt-${image.id}`}>Mô tả hỗ trợ đọc màn hình</label>
        <input
          id={`image-alt-${image.id}`}
          value={altText}
          onChange={(event) => setAltText(event.target.value)}
          maxLength={500}
        />
        <label className="checkbox-label" htmlFor={`image-cover-${image.id}`}>
          <input
            id={`image-cover-${image.id}`}
            type="checkbox"
            checked={coverCandidate}
            onChange={(event) => setCoverCandidate(event.target.checked)}
          />
          Cho phép dùng làm ảnh bìa
        </label>
        <div className="content-item-actions">
          <button type="submit" disabled={busy}>Lưu</button>
          <button type="button" disabled={busy || first} onClick={() => onMove(-1)}>
            Lên
          </button>
          <button type="button" disabled={busy || last} onClick={() => onMove(1)}>
            Xuống
          </button>
          <button
            type="button"
            disabled={busy || coverAssetId === image.assetId}
            onClick={() => void setCover()}
          >
            Dùng làm ảnh bìa
          </button>
          <button className="danger-button" type="button" disabled={busy} onClick={() => void unlink()}>
            Gỡ ảnh
          </button>
        </div>
        {error ? <p className="form-note form-error">{error}</p> : null}
      </form>
    </article>
  );
}

function MemberAvatarEditor({
  memoryId,
  member,
  images,
  onUpdated,
}: {
  memoryId: string;
  member: MemoryMember;
  images: MemoryImage[];
  onUpdated: (member: MemoryMember) => void;
}) {
  const [assetId, setAssetId] = useState(member.avatarAssetId ?? "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      onUpdated(
        await requestJson<MemoryMember>(
          `/api/memories/${memoryId}/members/${member.id}/avatar`,
          "PUT",
          { assetId: assetId || null, version: member.version },
        ),
      );
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="avatar-row" onSubmit={save}>
      <label htmlFor={`member-avatar-${member.id}`}>{member.fullName}</label>
      <select
        id={`member-avatar-${member.id}`}
        value={assetId}
        onChange={(event) => setAssetId(event.target.value)}
      >
        <option value="">Không có avatar</option>
        {images.map((image) => (
          <option key={image.assetId} value={image.assetId}>
            {image.altText || image.caption || `Ảnh #${image.sortOrder}`}
          </option>
        ))}
      </select>
      <button type="submit" disabled={busy}>Lưu avatar</button>
      {error ? <p className="form-note form-error">{error}</p> : null}
    </form>
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
    throw new MediaProblem(problem.detail, problem.code);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

async function readProblem(response: Response) {
  try {
    const problem = (await response.json()) as { detail?: string; code?: string };
    return {
      code: problem.code,
      detail: problem.detail ?? "Không thể xử lý ảnh memory.",
    };
  } catch {
    return { detail: "Không thể xử lý ảnh memory." };
  }
}

function validateFile(file: File) {
  if (!ALLOWED_IMAGE_TYPES.includes(file.type) || file.size < 1 || file.size > MAX_FILE_SIZE) {
    throw new Error("Ảnh phải là JPEG, PNG, WebP hoặc AVIF và không vượt quá 10 MiB.");
  }
}

async function sha256Base64(file: File) {
  const digest = await crypto.subtle.digest("SHA-256", await file.arrayBuffer());
  return btoa(String.fromCharCode(...new Uint8Array(digest)));
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

function errorMessage(reason: unknown) {
  return reason instanceof Error ? reason.message : "Dịch vụ tạm thời không khả dụng.";
}
