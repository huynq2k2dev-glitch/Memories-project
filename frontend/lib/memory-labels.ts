import type { MemoryType } from "@/lib/api-types";

export const MEMORY_TYPES: MemoryType[] = [
  "WEDDING",
  "FUNERAL",
  "GRADUATION",
  "HOUSEWARMING",
  "PERSONAL",
];

export function memoryTypeLabel(type: MemoryType) {
  switch (type) {
    case "WEDDING":
      return "Lễ cưới";
    case "FUNERAL":
      return "Tưởng niệm";
    case "GRADUATION":
      return "Tốt nghiệp";
    case "HOUSEWARMING":
      return "Tân gia";
    case "PERSONAL":
      return "Kỷ niệm cá nhân";
  }
}

export function memoryStatusLabel(status: "DRAFT" | "PUBLISHED" | "ARCHIVED") {
  switch (status) {
    case "DRAFT":
      return "Bản nháp";
    case "PUBLISHED":
      return "Đã xuất bản";
    case "ARCHIVED":
      return "Đã lưu trữ";
  }
}
