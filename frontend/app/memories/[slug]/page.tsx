import { cookies } from "next/headers";
import { notFound } from "next/navigation";

import { selectMemoryAccessCookies } from "@/lib/backend-api";
import {
  type MemoryRenderPayload,
  RegisteredTemplateRenderer,
  supportsTemplateRenderer,
} from "@/templates/registry";
import MemoryUnlockForm from "./memory-unlock-form";
import GuestMessageSection from "./guest-message-section";
import GuestRsvpForm, {
  type GuestRsvp,
} from "../../invitations/[token]/guest-rsvp-form";

const BACKEND_URL = process.env.BACKEND_URL ?? "http://127.0.0.1:8080";

type ShareRsvpInvitation = {
  guest: {
    fullName: string;
    guestGroup: string | null;
    maxPartySize: number;
  };
  memory: {
    title: string;
  };
  events: Array<{
    id: string;
    eventType: string;
    title: string;
    description: string | null;
    startAt: string;
    endAt: string | null;
    timezone: string;
    sortOrder: number;
    rsvp: GuestRsvp | null;
  }>;
};

export default async function PublicMemoryPage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  const cookieHeader = selectMemoryAccessCookies((await cookies()).toString());
  const response = await fetch(
    `${BACKEND_URL}/api/v1/public/memories/${encodeURIComponent(slug)}`,
    {
      headers: cookieHeader ? { Cookie: cookieHeader } : undefined,
      cache: "no-store",
    },
  );
  if (response.status === 401 && (await isMemoryAccessRequired(response))) {
    return <MemoryUnlockForm slug={slug} />;
  }
  if (response.status === 404) {
    notFound();
  }
  if (!response.ok) {
    throw new Error("Public memory is temporarily unavailable.");
  }
  const payload = (await response.json()) as MemoryRenderPayload;
  const shareRsvp = cookieHeader
    ? await fetchShareRsvp(slug, cookieHeader)
    : null;

  if (!supportsTemplateRenderer(payload.componentKey, payload.rendererVersion)) {
    return (
      <main className="public-memory-shell">
        <h1>Renderer chưa tương thích</h1>
        <p>Frontend build hiện tại chưa thể hiển thị phiên bản memory này.</p>
      </main>
    );
  }

  return (
    <main className="public-memory-shell">
      <RegisteredTemplateRenderer
        componentKey={payload.componentKey}
        rendererVersion={payload.rendererVersion}
        payload={payload}
        slots={payload.componentKey === "html-book" ? {
          RSVP: shareRsvp ? <ShareRsvpSection slug={slug} invitation={shareRsvp} /> : <p>Mở liên kết lời mời riêng để xác nhận tham dự.</p>,
          GUEST_MESSAGES: <GuestMessageSection slug={slug} messages={payload.messages} canSubmit={payload.visibility !== "PRIVATE"} />,
        } : undefined}
      />
      {payload.componentKey !== "html-book" && shareRsvp ? (
        <ShareRsvpSection slug={slug} invitation={shareRsvp} />
      ) : null}
      {payload.componentKey !== "html-book" ? <GuestMessageSection
        slug={slug}
        messages={payload.messages}
        canSubmit={payload.visibility !== "PRIVATE"}
      /> : null}
    </main>
  );
}

async function fetchShareRsvp(slug: string, cookieHeader: string) {
  const response = await fetch(
    `${BACKEND_URL}/api/v1/public/memories/${encodeURIComponent(slug)}/share-rsvp`,
    {
      headers: { Cookie: cookieHeader },
      cache: "no-store",
    },
  );
  if (response.status === 404) {
    return null;
  }
  if (!response.ok) {
    throw new Error("Share RSVP is temporarily unavailable.");
  }
  return (await response.json()) as ShareRsvpInvitation;
}

function ShareRsvpSection({
  slug,
  invitation,
}: {
  slug: string;
  invitation: ShareRsvpInvitation;
}) {
  const responsePath = `/api/v1/public/memories/${encodeURIComponent(slug)}/share-rsvp/responses`;
  return (
    <section className="invitation-events share-rsvp-section" aria-label="RSVP">
      <h2>Phản hồi tham dự cho {invitation.guest.fullName}</h2>
      {invitation.guest.guestGroup ? (
        <p>Nhóm khách: {invitation.guest.guestGroup}</p>
      ) : null}
      <p>Số người tối đa: {invitation.guest.maxPartySize}</p>
      {invitation.events.length === 0 ? (
        <p>Hiện chưa có sự kiện nhận RSVP.</p>
      ) : null}
      {invitation.events.map((event) => (
        <article key={event.id}>
          <p className="template-code">{event.eventType}</p>
          <h3>{event.title}</h3>
          <p>{formatEventTime(event.startAt, event.endAt, event.timezone)}</p>
          {event.description ? <p>{event.description}</p> : null}
          <GuestRsvpForm
            responsePath={responsePath}
            eventId={event.id}
            maxPartySize={invitation.guest.maxPartySize}
            initialResponse={event.rsvp}
          />
        </article>
      ))}
    </section>
  );
}

function formatEventTime(
  startAt: string,
  endAt: string | null,
  timezone: string,
) {
  const formatter = new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "full",
    timeStyle: "short",
    timeZone: timezone,
  });
  const start = formatter.format(new Date(startAt));
  return endAt ? `${start} – ${formatter.format(new Date(endAt))}` : start;
}

async function isMemoryAccessRequired(response: Response) {
  try {
    const problem = (await response.json()) as { code?: string };
    return problem.code === "MEMORY_ACCESS_REQUIRED";
  } catch {
    return false;
  }
}
