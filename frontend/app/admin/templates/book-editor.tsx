"use client";

import { useId, useState } from "react";
import HtmlBookRenderer from "@/templates/html-book-renderer";
import { BOOK_PAGE_TYPES, parseBook, type HtmlBook, type HtmlBookPage } from "@/templates/html-book-contract";
import { bookPreview } from "@/templates/book-preview";
import styles from "./book-editor.module.css";

export function newBook(): HtmlBook {
  return {
    config: { background: "#e8e1d4", paper: "#fffaf0", direction: "ltr", effect: "flip", desktopSpread: true, aspectRatio: 0.72 },
    css: ".cover { text-align: center; padding: 40px 8px; }\n.eyebrow { letter-spacing: 4px; text-transform: uppercase; font-size: 12px; }",
    pages: [
      { key: "cover", name: "Bìa", type: "COVER", html: '<section class="cover"><p class="eyebrow">Thiệp mời thành hôn</p><h1 data-bind="title"></h1><p data-bind="eventStartAt" data-format="date"></p><p data-bind="summary"></p></section>' },
      { key: "invitation", name: "Lời mời", type: "CONTENT", html: '<section><h2>Trân trọng kính mời</h2><p data-bind="summary"></p><div data-repeat="members"><h3 data-bind="item.fullName"></h3><p data-bind="item.description"></p></div><div data-repeat="events"><h3 data-bind="item.title"></h3><p data-bind="item.startAt" data-format="date"></p></div><div data-slot="MAP"></div></section>' },
      { key: "rsvp", name: "Hẹn gặp bạn", type: "CLOSING", html: '<section><h2>Hẹn gặp bạn trong ngày vui</h2><div data-slot="RSVP"></div><div data-slot="GUEST_MESSAGES"></div></section>' },
    ],
  };
}

export default function BookEditor({ book, onChange, themeConfig }: { book: HtmlBook; onChange: (book: HtmlBook) => void; themeConfig: string }) {
  const id = useId();
  const [selected, setSelected] = useState(0);
  const [dragged, setDragged] = useState<number | null>(null);
  const [preview, setPreview] = useState<ReturnType<typeof bookPreview> | null>(null);
  const [error, setError] = useState("");
  const index = Math.min(selected, book.pages.length - 1);
  const page = book.pages[index];
  function updatePage(change: Partial<HtmlBookPage>) {
    onChange({ ...book, pages: book.pages.map((entry, position) => position === index ? { ...entry, ...change } : entry) });
  }
  function move(from: number, to: number) {
    if (from === 0 || to === 0 || to < 0 || to >= book.pages.length) return;
    const pages = [...book.pages];
    const [entry] = pages.splice(from, 1);
    pages.splice(to, 0, entry);
    onChange({ ...book, pages });
    setSelected(to);
  }
  function showPreview() {
    try {
      parseBook(book);
      const config = JSON.parse(themeConfig) as unknown;
      if (!config || typeof config !== "object" || Array.isArray(config)) throw new Error("Default config phải là object JSON.");
      setPreview(bookPreview(book, config as Record<string, unknown>));
      setError("");
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Mẫu không hợp lệ."); }
  }
  return <section className={styles.editor} aria-label="Thiết kế sách thiệp">
    <h3>Thiết kế từng trang</h3>
    <div className={styles.config}>
      <label>Màu nền<input type="color" value={book.config.background} onChange={(e) => onChange({ ...book, config: { ...book.config, background: e.target.value } })} /></label>
      <label>Màu giấy<input type="color" value={book.config.paper} onChange={(e) => onChange({ ...book, config: { ...book.config, paper: e.target.value } })} /></label>
      <label>Hướng lật<select value={book.config.direction} onChange={(e) => onChange({ ...book, config: { ...book.config, direction: e.target.value as "ltr" | "rtl" } })}><option value="ltr">Trái sang phải</option><option value="rtl">Phải sang trái</option></select></label>
      <label>Hiệu ứng<select value={book.config.effect} onChange={(e) => onChange({ ...book, config: { ...book.config, effect: e.target.value as "flip" | "none" } })}><option value="flip">Lật trang</option><option value="none">Không chuyển động</option></select></label>
      <label>Tỷ lệ rộng/cao<input type="number" min="0.6" max="1" step="0.01" value={book.config.aspectRatio} onChange={(e) => onChange({ ...book, config: { ...book.config, aspectRatio: e.target.valueAsNumber } })} /></label>
      <label><input type="checkbox" checked={book.config.desktopSpread} onChange={(e) => onChange({ ...book, config: { ...book.config, desktopSpread: e.target.checked } })} />Hai trang trên màn hình rộng</label>
    </div>
    <div className={styles.workspace}>
      <div>
        <ol className={styles.pages}>
          {book.pages.map((entry, position) => <li key={position} draggable={position > 0}
            onDragStart={() => setDragged(position)} onDragEnd={() => setDragged(null)}
            onDragOver={(e) => { if (position > 0) e.preventDefault(); }}
            onDrop={(e) => { e.preventDefault(); if (dragged !== null) move(dragged, position); setDragged(null); }}>
            <button type="button" aria-pressed={index === position} onClick={() => setSelected(position)}>{position + 1}. {entry.name}</button>
            {position > 0 ? <span className={styles.reorder}>
              <button type="button" aria-label={`Đưa ${entry.name} lên`} disabled={position === 1} onClick={() => move(position, position - 1)}>↑</button>
              <button type="button" aria-label={`Đưa ${entry.name} xuống`} disabled={position === book.pages.length - 1} onClick={() => move(position, position + 1)}>↓</button>
            </span> : null}
          </li>)}
        </ol>
        <button type="button" disabled={book.pages.length >= 16} onClick={() => {
          let suffix = book.pages.length + 1;
          while (book.pages.some((entry) => entry.key === `page-${suffix}`)) suffix++;
          onChange({ ...book, pages: [...book.pages, { key: `page-${suffix}`, name: "Trang mới", type: "CONTENT", html: "<section><h2>Trang mới</h2><p>Nội dung thiệp</p></section>" }] });
          setSelected(book.pages.length);
        }}>Thêm trang</button>
      </div>
      {page ? <div className={styles.fields}>
        <label htmlFor={`${id}-name`}>Tên trang</label><input id={`${id}-name`} value={page.name} maxLength={120} onChange={(e) => updatePage({ name: e.target.value })} />
        <label htmlFor={`${id}-key`}>Mã trang</label><input id={`${id}-key`} value={page.key} maxLength={60} onChange={(e) => updatePage({ key: e.target.value })} />
        <label htmlFor={`${id}-type`}>Loại trang</label><select id={`${id}-type`} value={page.type} disabled={index === 0} onChange={(e) => updatePage({ type: e.target.value })}>
          {BOOK_PAGE_TYPES.filter((type) => index === 0 || type !== "COVER").map((type) => <option key={type}>{type}</option>)}
        </select>
        <label htmlFor={`${id}-html`}>HTML</label><textarea id={`${id}-html`} spellCheck={false} rows={14} maxLength={20000} value={page.html} onChange={(e) => updatePage({ html: e.target.value })} />
        <button type="button" disabled={index === 0 || book.pages.length <= 2} onClick={() => {
          onChange({ ...book, pages: book.pages.filter((_, position) => position !== index) }); setSelected(Math.max(0, index - 1));
        }}>Xóa trang đang chọn</button>
      </div> : null}
    </div>
    <label htmlFor={`${id}-css`}>CSS dùng chung cho phiên bản</label>
    <textarea id={`${id}-css`} className={styles.css} rows={10} maxLength={20000} spellCheck={false} value={book.css} onChange={(e) => onChange({ ...book, css: e.target.value })} />
    <details><summary>Hướng dẫn HTML và dữ liệu</summary>
      <p>Dùng HTML có thẻ đóng đầy đủ; ảnh viết dạng <code>&lt;img data-src="cover.deliveryUrl" /&gt;</code>. Dùng <code>&amp;amp;</code> cho ký tự &amp;.</p>
      <p>Văn bản: <code>data-bind="title"</code>, <code>summary</code>, <code>eventStartAt</code>, <code>themeConfig.subtitle</code>. Ngày giờ: thêm <code>data-format="date"</code>.</p>
      <p>Danh sách: <code>data-repeat="members|sections|images|events|locations"</code> (chọn một). Bên trong dùng <code>data-bind="item.fullName"</code>, <code>item.contentText</code>, <code>item.caption</code>, <code>item.title</code>… Ảnh album: <code>data-src="item.asset.deliveryUrl"</code>. Link địa điểm: <code>data-href="item.mapUrl"</code>.</p>
      <p>Phải có đúng một <code>&lt;div data-slot="RSVP"&gt;&lt;/div&gt;</code> và một <code>&lt;div data-slot="GUEST_MESSAGES"&gt;&lt;/div&gt;</code>. Có thể thêm slot <code>MAP</code>. Slot không chứa nội dung hoặc thuộc tính khác.</p>
      <p>CSS dùng selector class chữ thường, ví dụ <code>.cover .title</code>. Hỗ trợ màu, gradient, font hệ thống, khoảng cách, viền, grid/flex. Không hỗ trợ script, URL tài nguyên, import CSS hoặc event handler.</p>
    </details>
    <button type="button" onClick={showPreview}>Kiểm tra nội dung và xem trước</button>
    {error ? <p role="alert">{error}</p> : null}
    {preview ? <div className={styles.preview}><p>Nội dung minh họa · Bấm xem trước để cập nhật các chỉnh sửa mới.</p><HtmlBookRenderer payload={preview} /></div> : null}
  </section>;
}
