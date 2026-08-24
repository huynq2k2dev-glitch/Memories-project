import { notFound } from "next/navigation";

import GuestRsvpForm, { type GuestRsvp } from "./guest-rsvp-form";

const BACKEND_URL = process.env.BACKEND_URL ?? "http://127.0.0.1:8080";

type GuestInvitation = {
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

export default async function GuestInvitationPage({
  params,
}: {
  params: Promise<{ token: string }>;
}) {
  const { token } = await params;
  const response = await fetch(
    `${BACKEND_URL}/api/v1/public/guests/${encodeURIComponent(token)}`,
    { cache: "no-store" },
  );
  if (response.status === 404) {
    notFound();
  }
  if (!response.ok) {
    throw new Error("Guest invitation is temporarily unavailable.");
  }
  const invitation = (await response.json()) as GuestInvitation;

  return (
    <main className="invitation-page">
      <section className="invitation-card">
        <p className="template-code">Lời mời dành riêng cho</p>
        <h1>{invitation.guest.fullName}</h1>
        <h2>{invitation.memory.title}</h2>
        {invitation.guest.guestGroup ? (
          <p>Nhóm khách: {invitation.guest.guestGroup}</p>
        ) : null}
        <p>Số người tối đa: {invitation.guest.maxPartySize}</p>
      </section>

      <section className="invitation-events" aria-label="Sự kiện có RSVP">
        <h2>Sự kiện</h2>
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
              responsePath={`/api/v1/public/guests/${encodeURIComponent(token)}/responses`}
              eventId={event.id}
              maxPartySize={invitation.guest.maxPartySize}
              initialResponse={event.rsvp}
            />
          </article>
        ))}
      </section>
    </main>
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
