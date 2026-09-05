import type { MemoryType } from "./api-types";

export const OCCASIONS = {
  PERSONAL: {
    label: "Kỷ niệm cá nhân", symbol: "✦", color: "#91613c",
    description: "Một chuyến đi, một cột mốc hay những ngày bình dị.",
    titleLabel: "Bạn muốn gọi kỷ niệm này là gì?", example: "Mùa hè của chúng mình",
    storyLabel: "Câu chuyện của bạn", storyHint: "Điều gì khiến khoảnh khắc này đáng nhớ?",
    dateLabel: "Ngày kỷ niệm (không bắt buộc)",
    peopleLabel: "Những người trong câu chuyện", scheduleLabel: "Mốc thời gian và địa điểm",
    messageLabel: "Lời nhắn", sample: "Có những ngày thật giản dị, nhưng mỗi lần nhớ lại đều khiến mình mỉm cười.",
    invitation: false,
  },
  WEDDING: {
    label: "Lễ cưới", symbol: "♡", color: "#a34f64",
    description: "Kể câu chuyện tình yêu và gửi lời mời tới người thân.",
    titleLabel: "Tên cặp đôi hoặc tên thiệp", example: "Minh & An — Ngày chung đôi",
    storyLabel: "Lời ngỏ", storyHint: "Viết đôi lời chào đón những người sẽ chung vui cùng hai bạn.",
    dateLabel: "Ngày giờ lễ cưới (có thể bổ sung sau)",
    peopleLabel: "Cặp đôi", scheduleLabel: "Lịch trình và địa điểm lễ cưới",
    messageLabel: "Lời chúc cưới", sample: "Chúng mình sắp bắt đầu một hành trình mới. Sự hiện diện của bạn sẽ khiến ngày ấy trọn vẹn hơn.",
    invitation: true,
  },
  FUNERAL: {
    label: "Tưởng niệm", symbol: "❧", color: "#59685e",
    description: "Một không gian trang trọng để lưu giữ và tưởng nhớ.",
    titleLabel: "Tên trang tưởng niệm", example: "Thương nhớ người thân yêu",
    storyLabel: "Lời tưởng nhớ", storyHint: "Chia sẻ về cuộc đời, những điều đáng quý và ký ức bạn muốn giữ lại.",
    dateLabel: "Ngày tưởng niệm (không bắt buộc)",
    peopleLabel: "Người được tưởng niệm", scheduleLabel: "Thông tin lễ tưởng niệm",
    messageLabel: "Lời tưởng niệm", sample: "Những điều tốt đẹp người để lại sẽ luôn ở trong ký ức và tình yêu thương của chúng ta.",
    invitation: false,
  },
  GRADUATION: {
    label: "Tốt nghiệp", symbol: "✧", color: "#38577d",
    description: "Lưu lại hành trình trưởng thành và một khởi đầu mới.",
    titleLabel: "Tên trang kỷ niệm tốt nghiệp", example: "Thanh An — Một chặng đường đáng nhớ",
    storyLabel: "Lời nhắn cho chặng đường đã qua", storyHint: "Nhắc đến trường, lớp, niên khóa và những người đã đồng hành cùng bạn.",
    dateLabel: "Ngày tốt nghiệp (có thể bổ sung sau)",
    peopleLabel: "Những người tốt nghiệp", scheduleLabel: "Lễ tốt nghiệp và buổi gặp mặt",
    messageLabel: "Lời chúc tốt nghiệp", sample: "Khép lại những năm tháng học trò, mang theo tình bạn và những ước mơ để bước tiếp.",
    invitation: false,
  },
  HOUSEWARMING: {
    label: "Tân gia", symbol: "⌂", color: "#927035",
    description: "Chào đón người thân, bạn bè đến với tổ ấm mới.",
    titleLabel: "Tên lời mời tân gia", example: "Mời bạn đến tổ ấm của chúng mình",
    storyLabel: "Lời mời", storyHint: "Gửi lời chào và những điều khách cần biết khi đến chung vui.",
    dateLabel: "Ngày giờ tân gia (có thể bổ sung sau)",
    peopleLabel: "Gia chủ", scheduleLabel: "Giờ đón khách và địa chỉ nhà",
    messageLabel: "Lời chúc tân gia", sample: "Nhà mới sẽ ấm áp hơn khi có những người thân yêu. Mời bạn đến chung vui cùng gia đình!",
    invitation: true,
  },
} satisfies Record<MemoryType, {
  label: string; symbol: string; color: string; description: string;
  titleLabel: string; example: string; storyLabel: string; storyHint: string;
  dateLabel: string; peopleLabel: string; scheduleLabel: string;
  messageLabel: string; sample: string; invitation: boolean;
}>;

export function occasionDesign(type: string) {
  return OCCASIONS[type as MemoryType] ?? OCCASIONS.PERSONAL;
}

export function sectionLabel(type: string) {
  const labels: Record<string, string> = {
    HERO: "Lời mở đầu", STORY: "Câu chuyện", GALLERY: "Album ảnh",
    TIMELINE: "Dấu mốc", TEXT: "Đoạn văn", SCHEDULE: "Lịch trình",
    LOCATION: "Địa điểm", RSVP: "Xác nhận tham dự", GUESTBOOK: "Lời nhắn",
  };
  return labels[type] ?? "Nội dung bổ sung";
}
