export type CurrentAccount = {
  id: string;
  email: string;
  displayName: string;
  status: "PENDING_VERIFICATION" | "ACTIVE" | "LOCKED" | "DELETED";
};

export type MemoryType =
  | "WEDDING"
  | "FUNERAL"
  | "GRADUATION"
  | "HOUSEWARMING"
  | "PERSONAL";

export type MemorySummary = {
  id: string;
  title: string;
  memoryType: MemoryType;
  status: "DRAFT" | "PUBLISHED" | "ARCHIVED";
  slug: string;
  cover: {
    id: string;
    mimeType: string;
    deliveryUrl: string;
  } | null;
  updatedAt: string;
};

export type MemoryPage = {
  items: MemorySummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};
