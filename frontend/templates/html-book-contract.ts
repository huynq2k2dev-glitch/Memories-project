export type BookSlot = "RSVP" | "GUEST_MESSAGES" | "MAP";
export type HtmlBookPage = { key: string; name: string; type: string; html: string };
export type HtmlBook = {
  config: {
    background: string;
    paper: string;
    direction: "ltr" | "rtl";
    effect: "flip" | "none";
    desktopSpread: boolean;
    aspectRatio: number;
  };
  css: string;
  pages: HtmlBookPage[];
};

export type BookNode = string | {
  tag: string;
  attrs: Record<string, string>;
  children: BookNode[];
};

const tags = new Set("section div header footer article h1 h2 h3 h4 p span strong em small br hr img figure figcaption ul ol li a time blockquote".split(" "));
const rootFields = new Set(["title", "summary", "eventStartAt", "cover.deliveryUrl"]);
const fields: Record<string, string[]> = {
  members: ["fullName", "displayName", "description", "avatar.deliveryUrl", "roleCode"],
  sections: ["title", "contentText"],
  images: ["caption", "altText", "asset.deliveryUrl"],
  events: ["title", "description", "startAt", "endAt", "timezone"],
  locations: ["name", "address", "mapUrl"],
};
export const BOOK_PAGE_TYPES = ["COVER", "CONTENT", "GALLERY", "SCHEDULE", "LOCATION", "RSVP", "GUEST_MESSAGES", "CLOSING"];
const properties = new Set("color background background-color border border-top border-bottom border-left border-right border-color border-width border-style border-radius padding padding-top padding-bottom padding-left padding-right margin margin-top margin-bottom margin-left margin-right font-family font-size font-weight font-style line-height letter-spacing text-align text-transform text-decoration display gap row-gap column-gap grid-template-columns align-items justify-content flex-direction flex-wrap width max-width min-height height max-height object-fit object-position aspect-ratio box-shadow white-space overflow-wrap".split(" "));
const functions = new Set(["rgb", "rgba", "hsl", "hsla", "linear-gradient", "radial-gradient", "repeat", "minmax", "clamp", "calc", "min", "max"]);

function requireValue(condition: unknown, message: string): asserts condition {
  if (!condition) throw new Error(message);
}

function isField(field: string, context?: string) {
  return rootFields.has(field) || /^themeConfig\.[a-zA-Z][a-zA-Z0-9]{0,49}$/.test(field)
    || !!(context && field.startsWith("item.") && fields[context]?.includes(field.slice(5)));
}

// Class names are renamed as well as scoped: template CSS never targets system forms.
export function scopedBookCss(css: string, scope: string): string {
  requireValue(typeof css === "string" && css.length <= 20000, "CSS tối đa 20.000 ký tự.");
  let remaining = css.trim();
  const rules: string[] = [];
  while (remaining) {
    const open = remaining.indexOf("{");
    const close = remaining.indexOf("}");
    requireValue(open > 0 && close > open, "CSS cần có dạng .ten-lop { thuộc-tính: giá-trị; }.");
    const selector = remaining.slice(0, open).trim();
    requireValue(/^\.[a-z][a-z0-9-]*( +\.[a-z][a-z0-9-]*)*$/.test(selector), "CSS chỉ hỗ trợ selector theo class, phân cách bằng dấu cách.");
    const declarations = remaining.slice(open + 1, close);
    for (const declaration of declarations.split(";")) {
      if (!declaration.trim()) continue;
      const colon = declaration.indexOf(":");
      const property = declaration.slice(0, colon).trim();
      const value = declaration.slice(colon + 1).trim();
      requireValue(colon > 0 && properties.has(property) && /^[a-zA-Z0-9#.,% ()/+*'"-]+$/.test(value), `CSS không hỗ trợ: ${property}`);
      for (const match of value.matchAll(/([a-zA-Z-]+)\s*\(/g)) {
        requireValue(functions.has(match[1]), "CSS chứa hàm không được hỗ trợ.");
      }
    }
    rules.push(`.${scope} ${selector.replace(/\.([a-z][a-z0-9-]*)/g, `.${scope}-$1`)} {${declarations}}`);
    remaining = remaining.slice(close + 1).trim();
  }
  return rules.join("\n");
}

export function parseBook(book: HtmlBook): BookNode[][] {
  requireValue(book?.config && Array.isArray(book.pages), "Thiếu cấu hình sách.");
  const { config } = book;
  requireValue(/^#[0-9a-fA-F]{6}$/.test(config.background) && /^#[0-9a-fA-F]{6}$/.test(config.paper), "Màu nền cần là mã #RRGGBB.");
  requireValue(["ltr", "rtl"].includes(config.direction) && ["flip", "none"].includes(config.effect), "Cấu hình lật trang không hợp lệ.");
  requireValue(Number.isFinite(config.aspectRatio) && config.aspectRatio >= 0.6 && config.aspectRatio <= 1, "Tỷ lệ trang từ 0,6 đến 1.");
  requireValue(book.pages.length >= 2 && book.pages.length <= 16, "Sách cần từ 2 đến 16 trang.");
  scopedBookCss(book.css, "preview");
  const keys = new Set<string>();
  const slots = new Set<string>();
  let total = book.css.length;
  const result = book.pages.map((page, index) => {
    requireValue(page && /^[a-z][a-z0-9-]{0,59}$/.test(page.key) && !keys.has(page.key), "Mã trang phải duy nhất, dùng chữ thường và dấu gạch ngang.");
    keys.add(page.key);
    requireValue(page.name?.trim() && page.name.length <= 120, "Tên trang tối đa 120 ký tự.");
    requireValue(BOOK_PAGE_TYPES.includes(page.type) && (index === 0 ? page.type === "COVER" : page.type !== "COVER"), "Trang đầu phải là bìa; các trang sau dùng loại khác.");
    requireValue(typeof page.html === "string" && page.html.trim() && page.html.length <= 20000, "HTML mỗi trang tối đa 20.000 ký tự.");
    total += page.html.length;
    requireValue(!/<!|<\?/.test(page.html), "HTML chỉ chứa thẻ và văn bản, không hỗ trợ khai báo hoặc comment.");
    const document = new DOMParser().parseFromString(`<root>${page.html}</root>`, "application/xml");
    requireValue(!document.querySelector("parsererror"), `HTML trang ${index + 1} chưa hợp lệ. Đóng thẻ, dùng <img /> và &amp;.`);
    let count = 0;
    function node(source: Node, context?: string, depth = 0): BookNode {
      requireValue(++count <= 500 && depth <= 20, "HTML quá nhiều thẻ hoặc lồng quá sâu.");
      if (source.nodeType === 3) return source.textContent ?? "";
      requireValue(source.nodeType === 1, "Chỉ hỗ trợ thẻ và văn bản.");
      const element = source as Element;
      const tag = element.tagName;
      requireValue(tags.has(tag), `Thẻ không hỗ trợ: ${tag}`);
      const attrs = Object.fromEntries(Array.from(element.attributes, (attr) => [attr.name, attr.value]));
      if (attrs["data-repeat"] !== undefined) {
        requireValue(!context && Object.hasOwn(fields, attrs["data-repeat"]), "Danh sách không hỗ trợ lồng nhau.");
        context = attrs["data-repeat"];
      }
      for (const [key, value] of Object.entries(attrs)) {
        switch (key) {
          case "class": requireValue(/^[a-z][a-z0-9-]*( [a-z][a-z0-9-]*)*$/.test(value), "Class dùng chữ thường và dấu gạch ngang."); break;
          case "alt": requireValue(tag === "img" && value.length <= 255, "Alt ảnh không hợp lệ."); break;
          case "data-repeat": break;
          case "data-bind": case "data-alt": requireValue(isField(value, context), `Binding không hỗ trợ: ${value}`); break;
          case "data-src": requireValue(tag === "img" && isField(value, context) && value.endsWith(".deliveryUrl"), "Ảnh cần bind URL media."); break;
          case "data-href": requireValue(tag === "a" && context === "locations" && value === "item.mapUrl", "Link chỉ hỗ trợ item.mapUrl."); break;
          case "data-format": requireValue(value === "date" && attrs["data-bind"], "Format chỉ hỗ trợ date."); break;
          case "data-slot":
            requireValue(tag === "div" && !context && ["RSVP", "GUEST_MESSAGES", "MAP"].includes(value) && !slots.has(value)
              && !element.hasChildNodes() && Object.keys(attrs).length === 1, "Slot cần là div trống, duy nhất và nằm ngoài danh sách.");
            slots.add(value); break;
          default: throw new Error(`Thuộc tính không hỗ trợ: ${key}`);
        }
      }
      requireValue(tag !== "img" || attrs["data-src"], "Ảnh cần data-src.");
      requireValue(!attrs["data-bind"] || !element.hasChildNodes(), "Thẻ data-bind phải trống.");
      requireValue(!["img", "br", "hr"].includes(tag) || !element.hasChildNodes(), "Thẻ img/br/hr phải tự đóng.");
      return { tag, attrs, children: Array.from(element.childNodes, (child) => node(child, context, depth + 1)) };
    }
    return Array.from(document.documentElement.childNodes, (child) => node(child));
  });
  requireValue(total <= 120000, "Tổng HTML/CSS tối đa 120.000 ký tự.");
  requireValue(slots.has("RSVP") && slots.has("GUEST_MESSAGES"), "Cần một slot RSVP và một slot GUEST_MESSAGES.");
  return result;
}

export function safeBookUrl(value: unknown): string | undefined {
  if (typeof value !== "string" || /[\s\\\u0000-\u001f]/.test(value)) return undefined;
  if (/^\/(?!\/)/.test(value)) return value;
  try {
    const url = new URL(value);
    return ["http:", "https:"].includes(url.protocol) && !url.username && !url.password ? value : undefined;
  } catch { return undefined; }
}
