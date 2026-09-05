import type { HtmlBook } from "./html-book-contract";
import type { MemoryRenderPayload } from "./registry";

export function bookPreview(book: HtmlBook, themeConfig: Record<string, unknown> = {}): MemoryRenderPayload {
  return {
    slug: "template-preview", title: "Minh & An", memoryType: "WEDDING", status: "DRAFT", visibility: "PRIVATE",
    summary: "Trân trọng mời bạn đến chung vui trong ngày chúng mình về chung một nhà.",
    themeConfig, eventStartAt: "2026-12-20T10:00:00+07:00", publishedAt: null, expiresAt: null,
    templateVersionId: "book-preview", componentKey: "html-book", rendererVersion: "1", book, cover: null,
    members: [
      { id: "bride", roleCode: "BRIDE", fullName: "Nguyễn An", displayName: "An", description: "Một người thích những buổi sáng bình yên và những chuyến đi cùng nhau.", avatar: null, sortOrder: 0 },
      { id: "groom", roleCode: "GROOM", fullName: "Trần Minh", displayName: "Minh", description: "Cùng nhau viết tiếp những ngày thật đẹp.", avatar: null, sortOrder: 1 },
    ],
    sections: [{ id: "story", sectionKey: "story", sectionType: "STORY", title: "Chuyện chúng mình", contentText: "Từ lần gặp đầu tiên đến lời hẹn ước hôm nay, mỗi chặng đường đều đẹp hơn khi có nhau.", config: {}, sortOrder: 0, required: false, contentComplete: true }],
    locations: [{ id: "venue", name: "Khu vườn Hạnh Phúc", address: "Địa chỉ tiệc cưới sẽ được cặp đôi cập nhật tại đây.", latitude: null, longitude: null, mapUrl: null, sortOrder: 0 }],
    events: [
      { id: "ceremony", locationId: "venue", eventType: "CEREMONY", title: "Lễ thành hôn", description: "Khoảnh khắc trao lời hẹn ước trước gia đình và bạn bè.", startAt: "2026-12-20T10:00:00+07:00", endAt: null, timezone: "Asia/Ho_Chi_Minh", sortOrder: 0, rsvpEnabled: true },
      { id: "party", locationId: "venue", eventType: "RECEPTION", title: "Tiệc chung vui", description: "Cùng nâng ly và lưu lại những kỷ niệm đáng nhớ.", startAt: "2026-12-20T11:30:00+07:00", endAt: null, timezone: "Asia/Ho_Chi_Minh", sortOrder: 1, rsvpEnabled: true },
    ], images: [], messages: [],
  };
}
