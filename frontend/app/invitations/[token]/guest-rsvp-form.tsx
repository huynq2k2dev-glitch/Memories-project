"use client";

import { FormEvent, useState } from "react";

export type GuestAttendanceStatus =
  | "PENDING"
  | "ATTENDING"
  | "DECLINED"
  | "MAYBE";

export type GuestRsvp = {
  attendanceStatus: GuestAttendanceStatus;
  partySize: number;
  dietaryNote: string | null;
  message: string | null;
  respondedAt: string | null;
  updatedAt: string;
  version: number;
};

type GuestRsvpResponse = GuestRsvp & {
  eventId: string;
};

const ATTENDANCE_OPTIONS: Array<{
  value: GuestAttendanceStatus;
  label: string;
}> = [
  { value: "ATTENDING", label: "Sẽ tham dự" },
  { value: "MAYBE", label: "Có thể tham dự" },
  { value: "DECLINED", label: "Không tham dự" },
  { value: "PENDING", label: "Chưa quyết định" },
];

export default function GuestRsvpForm({
  responsePath,
  eventId,
  maxPartySize,
  initialResponse,
}: {
  responsePath: string;
  eventId: string;
  maxPartySize: number;
  initialResponse: GuestRsvp | null;
}) {
  const [currentResponse, setCurrentResponse] = useState(initialResponse);
  const [attendanceStatus, setAttendanceStatus] =
    useState<GuestAttendanceStatus>(
      initialResponse?.attendanceStatus ?? "ATTENDING",
    );
  const [partySize, setPartySize] = useState(initialResponse?.partySize ?? 1);
  const [dietaryNote, setDietaryNote] = useState(
    initialResponse?.dietaryNote ?? "",
  );
  const [message, setMessage] = useState(initialResponse?.message ?? "");
  const [isSaving, setIsSaving] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  function changeAttendanceStatus(status: GuestAttendanceStatus) {
    setAttendanceStatus(status);
    if (status === "DECLINED") {
      setPartySize(0);
    } else if (partySize < 1) {
      setPartySize(1);
    }
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSaving(true);
    setNotice(null);
    setError(null);

    try {
      const response = await fetch(responsePath, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          eventId,
          attendanceStatus,
          partySize,
          dietaryNote,
          message,
          version: currentResponse?.version ?? null,
        }),
      });
      if (!response.ok) {
        const problem = (await response.json().catch(() => null)) as {
          detail?: string;
        } | null;
        if (response.status === 409) {
          throw new Error(
            "Phản hồi đã thay đổi ở nơi khác. Hãy tải lại trang trước khi gửi lại.",
          );
        }
        throw new Error(problem?.detail ?? "Không thể lưu phản hồi lúc này.");
      }

      const saved = (await response.json()) as GuestRsvpResponse;
      setCurrentResponse(saved);
      setNotice("Đã lưu phản hồi của bạn.");
    } catch (submitError) {
      setError(
        submitError instanceof Error
          ? submitError.message
          : "Không thể lưu phản hồi lúc này.",
      );
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <form className="guest-rsvp-form" onSubmit={submit}>
      <h4>Xác nhận tham dự</h4>
      <label>
        Phản hồi
        <select
          value={attendanceStatus}
          onChange={(event) =>
            changeAttendanceStatus(
              event.target.value as GuestAttendanceStatus,
            )
          }
          disabled={isSaving}
        >
          {ATTENDANCE_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </label>

      <label>
        Số người tham dự
        <input
          type="number"
          min={attendanceStatus === "DECLINED" ? 0 : 1}
          max={attendanceStatus === "DECLINED" ? 0 : maxPartySize}
          value={partySize}
          onChange={(event) => setPartySize(Number(event.target.value))}
          disabled={isSaving || attendanceStatus === "DECLINED"}
          required
        />
      </label>

      <label>
        Ghi chú ăn uống
        <textarea
          value={dietaryNote}
          onChange={(event) => setDietaryNote(event.target.value)}
          maxLength={500}
          rows={2}
          disabled={isSaving}
        />
      </label>

      <label>
        Lời nhắn
        <textarea
          value={message}
          onChange={(event) => setMessage(event.target.value)}
          maxLength={1000}
          rows={3}
          disabled={isSaving}
        />
      </label>

      <button type="submit" disabled={isSaving}>
        {isSaving ? "Đang lưu..." : "Lưu phản hồi"}
      </button>
      {notice ? (
        <p className="guest-rsvp-success" role="status">
          {notice}
        </p>
      ) : null}
      {error ? (
        <p className="guest-rsvp-error" role="alert">
          {error}
        </p>
      ) : null}
    </form>
  );
}
