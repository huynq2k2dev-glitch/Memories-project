"use client";

import { createElement, Fragment, useEffect, useId, useMemo, useRef, useState, useSyncExternalStore, type CSSProperties, type ReactNode } from "react";
import type { TemplateRendererProps } from "./registry";
import { parseBook, safeBookUrl, scopedBookCss, type BookNode, type BookSlot } from "./html-book-contract";
import styles from "./html-book.module.css";

const subscribe = () => () => {};
const clientSnapshot = () => true;
const serverSnapshot = () => false;

export default function HtmlBookRenderer({ payload, slots }: TemplateRendererProps) {
  const ready = useSyncExternalStore(subscribe, clientSnapshot, serverSnapshot);
  const scope = `book${useId().replace(/[^a-zA-Z0-9]/g, "")}`;
  const container = useRef<HTMLDivElement>(null);
  const touch = useRef<{ x: number; y: number } | null>(null);
  const [wide, setWide] = useState(false);
  const [page, setPage] = useState(0);
  const [reading, setReading] = useState(false);
  const [motion, setMotion] = useState("next");
  const book = payload.book;
  const parsed = useMemo(() => {
    if (!ready || !book) return null;
    try { return { pages: parseBook(book), css: scopedBookCss(book.css, scope), error: null }; }
    catch (error) { return { pages: [], css: "", error: error instanceof Error ? error.message : "Mẫu không hợp lệ." }; }
  }, [book, ready, scope]);

  useEffect(() => {
    if (!container.current) return;
    const observer = new ResizeObserver(([entry]) => setWide(entry.contentRect.width >= 850));
    observer.observe(container.current);
    return () => observer.disconnect();
  }, []);

  const spread = !!(wide && book?.config.desktopSpread);
  const count = book?.pages.length ?? 0;
  const boundedPage = Math.min(page, Math.max(0, count - 1));
  const start = spread && boundedPage > 0 ? 1 + Math.floor((boundedPage - 1) / 2) * 2 : boundedPage;
  const end = Math.min(count - 1, start + (spread && start > 0 ? 1 : 0));
  const rtl = book?.config.direction === "rtl";
  function turn(delta: number) {
    setMotion(delta > 0 ? "next" : "previous");
    setPage(delta > 0 ? Math.min(count - 1, end + 1) : Math.max(0, start - (spread && start > 1 ? 2 : 1)));
  }

  const interactiveSlots: Partial<Record<BookSlot, ReactNode>> = {
    RSVP: <p>Phần xác nhận tham dự sẽ hiển thị khi khách mở lời mời có quyền phản hồi.</p>,
    GUEST_MESSAGES: <div><h3>Sổ lời chúc</h3>{payload.messages.length ? payload.messages.map((message) => (
      <blockquote key={message.id}><p>{message.content}</p><cite>{message.guestName}</cite></blockquote>
    )) : <p>Lời chúc của người thân sẽ được lưu tại đây.</p>}</div>,
    MAP: <div>{payload.locations.map((location) => <p key={location.id}>
      <strong>{location.name}</strong><br />{location.address}<br />
      {safeBookUrl(location.mapUrl) ? <a href={safeBookUrl(location.mapUrl)} target="_blank" rel="noopener noreferrer">Mở bản đồ</a> : null}
    </p>)}</div>,
    ...slots,
  };

  function nodes(items: BookNode[], item?: unknown, prefix = "root"): ReactNode {
    return items.map((node, index) => {
      const key = `${prefix}-${index}`;
      if (typeof node === "string") return node;
      const repeat = node.attrs["data-repeat"];
      if (repeat) {
        const values = readValue(payload, repeat);
        const repeated = { ...node, attrs: { ...node.attrs } };
        delete repeated.attrs["data-repeat"];
        return Array.isArray(values) ? values.map((value, row) => <Fragment key={`${key}-${row}`}>
          {nodes([repeated], value, `${key}-${row}`)}
        </Fragment>) : null;
      }
      if (node.attrs["data-slot"]) return <div className={styles.slot} key={key}>{interactiveSlots[node.attrs["data-slot"] as BookSlot]}</div>;
      const props: Record<string, unknown> = { key };
      if (node.attrs.class) props.className = node.attrs.class.split(" ").map((name) => `${scope}-${name}`).join(" ");
      const value = (path: string) => path.startsWith("item.") ? readValue(item, path.slice(5)) : readValue(payload, path);
      if (node.tag === "img") {
        const src = safeBookUrl(value(node.attrs["data-src"]));
        if (!src) return <div className={styles.imagePlaceholder} key={key} aria-label="Chưa có ảnh">♡</div>;
        props.src = src;
        props.alt = textValue(node.attrs["data-alt"] ? value(node.attrs["data-alt"]) : node.attrs.alt ?? "Ảnh kỷ niệm");
        props.loading = "lazy";
        props.referrerPolicy = "no-referrer";
        props.onError = (event: { currentTarget: HTMLImageElement }) => { event.currentTarget.alt = "Ảnh tạm thời không khả dụng"; };
      }
      if (node.tag === "a") {
        props.href = safeBookUrl(value(node.attrs["data-href"]));
        props.target = "_blank";
        props.rel = "noopener noreferrer";
      }
      if (["img", "br", "hr"].includes(node.tag)) return createElement(node.tag, props);
      let children: ReactNode = nodes(node.children, item, key);
      if (node.attrs["data-bind"]) {
        const bound = value(node.attrs["data-bind"]);
        children = node.attrs["data-format"] === "date" ? formatDate(bound, readValue(item, "timezone")) : textValue(bound);
      }
      // Explicit props and React text nodes prevent saved markup from becoming executable HTML.
      return createElement(node.tag, props, children);
    });
  }

  return <div ref={container} className={`${styles.book} ${scope}`} style={book && parsed && !parsed.error ? {
    "--book-background": book.config.background,
    "--book-paper": book.config.paper,
    "--book-ratio": book.config.aspectRatio,
  } as CSSProperties : undefined}>
    {!book ? <p role="alert">Mẫu thiệp chưa có nội dung sách.</p> : !parsed ? <p role="status">Đang mở thiệp…</p> : parsed.error ? <p role="alert">Không thể hiển thị mẫu: {parsed.error}</p> : <>
      <style>{parsed.css}</style>
      <div className={styles.toolbar}>
        <span>Thiệp mời · {payload.title}</span>
        <button type="button" aria-pressed={reading} onClick={() => setReading(!reading)}>{reading ? "Xem dạng thiệp" : "Đọc liên tục"}</button>
      </div>
      <div className={styles.stage} data-reading={reading} data-spread={spread && start > 0} data-direction={rtl ? "rtl" : "ltr"}
        data-effect={book.config.effect} data-motion={motion} tabIndex={0} aria-label="Nội dung thiệp, dùng phím trái phải để lật trang"
        onKeyDown={(event) => {
          if (reading || isInteractive(event.target)) return;
          if (event.key === "ArrowRight" || event.key === "ArrowLeft") {
            event.preventDefault(); turn((event.key === "ArrowRight") !== rtl ? 1 : -1);
          }
        }}
        onTouchStart={(event) => {
          if (reading || isInteractive(event.target) || event.touches.length !== 1) { touch.current = null; return; }
          touch.current = { x: event.touches[0].clientX, y: event.touches[0].clientY };
        }}
        onTouchCancel={() => { touch.current = null; }}
        onTouchEnd={(event) => {
          if (!touch.current) return;
          const dx = event.changedTouches[0].clientX - touch.current.x;
          const dy = event.changedTouches[0].clientY - touch.current.y;
          touch.current = null;
          if (Math.abs(dx) > 75 && Math.abs(dx) > Math.abs(dy) * 1.5) turn((dx < 0) !== rtl ? 1 : -1);
        }}>
        {book.pages.map((entry, index) => <section key={entry.key} className={styles.sheet}
          hidden={!reading && (index < start || index > end)} aria-label={`${entry.name}, trang ${index + 1}`}
          data-side={index % 2 === 1 ? "left" : "right"}>
          <div className={styles.content}>{nodes(parsed.pages[index], undefined, entry.key)}</div>
          <span className={styles.pageNumber}>{String(index + 1).padStart(2, "0")}</span>
        </section>)}
      </div>
      {!reading ? <nav className={styles.controls} aria-label="Lật thiệp">
        <button type="button" disabled={start === 0} onClick={() => turn(-1)}>{rtl ? "→" : "←"} Trang trước</button>
        <span aria-live="polite" aria-atomic="true">{start === end ? start + 1 : `${start + 1}–${end + 1}`} / {count}</span>
        <button type="button" disabled={end === count - 1} onClick={() => turn(1)}>{start === 0 ? "Mở thiệp" : "Trang sau"} {rtl ? "←" : "→"}</button>
      </nav> : null}
      <div className={styles.pageLinks} aria-label="Chọn trang">
        {book.pages.map((entry, index) => <button key={entry.key} type="button" aria-current={!reading && index >= start && index <= end ? "page" : undefined}
          onClick={() => { setReading(false); setMotion(index > start ? "next" : "previous"); setPage(index); }}>{entry.name}</button>)}
      </div>
    </>}
  </div>;
}

function isInteractive(target: EventTarget) {
  return target instanceof Element && !!target.closest("input, textarea, select, button, a, [contenteditable]");
}

function readValue(object: unknown, path: string): unknown {
  if (!path) return undefined;
  return path.split(".").reduce<unknown>((value, key) => value !== null && typeof value === "object" && Object.hasOwn(value, key)
    ? (value as Record<string, unknown>)[key] : undefined, object);
}

function textValue(value: unknown) {
  return typeof value === "string" || typeof value === "number" ? String(value) : "";
}

function formatDate(value: unknown, timezone: unknown) {
  if (typeof value !== "string" || !value) return "";
  const date = new Date(value);
  if (!Number.isFinite(date.getTime())) return "";
  try { return new Intl.DateTimeFormat("vi-VN", { dateStyle: "long", timeStyle: "short", timeZone: typeof timezone === "string" ? timezone : "Asia/Ho_Chi_Minh" }).format(date); }
  catch { return new Intl.DateTimeFormat("vi-VN", { dateStyle: "long", timeStyle: "short", timeZone: "Asia/Ho_Chi_Minh" }).format(date); }
}
