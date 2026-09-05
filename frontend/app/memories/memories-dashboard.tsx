/* eslint-disable @next/next/no-img-element -- Cover URLs are short-lived runtime delivery URLs. */
"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";

import RequireAuth from "@/components/require-auth";
import type { MemoryPage } from "@/lib/api-types";
import { authenticatedFetch } from "@/lib/auth-session";
import { memoryStatusLabel, memoryTypeLabel } from "@/lib/memory-labels";

export default function MemoriesDashboard() {
  return (
    <RequireAuth>
      <DashboardContent />
    </RequireAuth>
  );
}

function DashboardContent() {
  const [memoryPage, setMemoryPage] = useState<MemoryPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(async (page: number) => {
    setLoading(true);
    setError("");
    try {
      setMemoryPage(await fetchMemoryPage(page));
    } catch (reason) {
      setError(
        reason instanceof Error
          ? reason.message
          : "Dịch vụ tạm thời không khả dụng.",
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let active = true;
    void fetchMemoryPage(0)
      .then((result) => {
        if (active) {
          setMemoryPage(result);
        }
      })
      .catch((reason: unknown) => {
        if (active) {
          setError(
            reason instanceof Error
              ? reason.message
              : "Dịch vụ tạm thời không khả dụng.",
          );
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

  return (
    <main className="page-shell dashboard-page">
      <header className="page-heading">
        <div>
          <p className="eyebrow">Không gian của bạn</p>
          <h1>Memory của tôi</h1>
          <p className="summary">Những câu chuyện bạn đã tạo, mới nhất ở phía trước.</p>
        </div>
        <Link className="primary-link" href="/memories/new">
          Tạo memory
        </Link>
      </header>

      {error ? (
        <section className="error-panel" role="alert">
          <p>{error}</p>
          <button type="button" onClick={() => void load(memoryPage?.page ?? 0)}>
            Thử lại
          </button>
        </section>
      ) : null}

      {loading ? <MemorySkeleton /> : null}

      {!loading && memoryPage?.items.length === 0 ? (
        <section className="empty-state">
          <p className="eyebrow">Trang đầu tiên đang chờ</p>
          <h2>Bắt đầu bằng một kỷ niệm đáng nhớ</h2>
          <p>Bạn chưa có memory nào. Chọn một mẫu và tạo câu chuyện đầu tiên.</p>
          <Link className="primary-link" href="/memories/new">
            Tạo memory đầu tiên
          </Link>
        </section>
      ) : null}

      {!loading && memoryPage?.items.length ? (
        <section className="memory-grid" aria-label="Danh sách memory">
          {memoryPage.items.map((memory) => (
            <article className="memory-card" key={memory.id}>
              <div className="memory-card-cover">
                {memory.cover ? (
                  <img src={memory.cover.deliveryUrl} alt={`Ảnh bìa ${memory.title}`} />
                ) : (
                  <span aria-hidden="true">M</span>
                )}
              </div>
              <div className="memory-card-body">
                <div className="memory-card-meta">
                  <span>{memoryTypeLabel(memory.memoryType)}</span>
                  <span>{memoryStatusLabel(memory.status)}</span>
                </div>
                <h2>{memory.title}</h2>
                <p>Cập nhật {formatUpdatedAt(memory.updatedAt)}</p>
                <Link className="secondary-link" href={`/memories/${memory.id}/edit`}>
                  Chỉnh sửa
                </Link>
              </div>
            </article>
          ))}
        </section>
      ) : null}

      {!loading && memoryPage && memoryPage.totalPages > 1 ? (
        <nav className="pagination dashboard-pagination" aria-label="Phân trang memory">
          <button
            type="button"
            disabled={memoryPage.page === 0}
            onClick={() => void load(memoryPage.page - 1)}
          >
            Trang trước
          </button>
          <span>
            Trang {memoryPage.page + 1}/{memoryPage.totalPages}
          </span>
          <button
            type="button"
            disabled={memoryPage.page + 1 >= memoryPage.totalPages}
            onClick={() => void load(memoryPage.page + 1)}
          >
            Trang sau
          </button>
        </nav>
      ) : null}
    </main>
  );
}

function MemorySkeleton() {
  return (
    <div className="memory-grid" aria-label="Đang tải memory" aria-busy="true">
      {[0, 1, 2].map((item) => (
        <div className="memory-card skeleton-card" key={item}>
          <div className="skeleton-block" />
          <div className="skeleton-lines">
            <span />
            <span />
            <span />
          </div>
        </div>
      ))}
    </div>
  );
}

function formatUpdatedAt(value: string) {
  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

async function fetchMemoryPage(page: number) {
  const response = await authenticatedFetch(`/api/memories?page=${page}&size=12`, {
    cache: "no-store",
  });
  if (!response.ok) {
    throw new Error("Chưa thể tải danh sách memory.");
  }
  return (await response.json()) as MemoryPage;
}
