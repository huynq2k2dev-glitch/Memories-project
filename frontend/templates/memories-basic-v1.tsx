/* eslint-disable @next/next/no-img-element -- Runtime media delivery URLs do not provide stable dimensions. */
import type { TemplateRendererProps } from "./registry";

export default function MemoriesBasicRenderer({
  payload,
}: TemplateRendererProps) {
  const accentColor = stringConfig(payload.themeConfig, "accentColor", "#9b4d54");
  const subtitle =
    payload.summary ??
    stringConfig(
      payload.themeConfig,
      "subtitle",
      "Nơi những khoảnh khắc quan trọng được lưu giữ.",
    );
  const ungroupedImages = payload.images.filter((image) => !image.sectionId);

  return (
    <article
      className="memory-renderer memory-render-page"
      style={{ borderColor: accentColor }}
    >
      <header className="render-hero">
        {payload.cover ? (
          <img
            className="render-cover"
            src={payload.cover.deliveryUrl}
            alt={`Ảnh bìa ${payload.title}`}
          />
        ) : null}
        <div>
          <p className="eyebrow">{payload.memoryType}</p>
          <h1>{payload.title}</h1>
          <p className="summary">{subtitle}</p>
        </div>
      </header>

      {payload.members.length ? (
        <section className="render-block">
          <h2>Nhân vật</h2>
          <div className="render-member-grid">
            {payload.members.map((member) => (
              <article className="render-card" key={member.id}>
                {member.avatar ? (
                  <img
                    className="render-avatar"
                    src={member.avatar.deliveryUrl}
                    alt={member.displayName ?? member.fullName}
                  />
                ) : null}
                <p className="eyebrow">{member.roleCode}</p>
                <h3>{member.displayName ?? member.fullName}</h3>
                {member.description ? <p>{member.description}</p> : null}
              </article>
            ))}
          </div>
        </section>
      ) : null}

      {payload.sections.map((section) => {
        const sectionImages = payload.images.filter(
          (image) => image.sectionId === section.id,
        );
        return (
          <section className="render-block" key={section.id}>
            <p className="eyebrow">{section.sectionType}</p>
            {section.title ? <h2>{section.title}</h2> : null}
            {section.contentText ? (
              <p className="render-copy">{section.contentText}</p>
            ) : null}
            <ImageGallery images={sectionImages} />
          </section>
        );
      })}

      <ImageGallery images={ungroupedImages} />

      {payload.events.length ? (
        <section className="render-block">
          <h2>Lịch sự kiện</h2>
          <div className="render-list">
            {payload.events.map((event) => (
              <article className="render-card" key={event.id}>
                <p className="eyebrow">{event.eventType}</p>
                <h3>{event.title}</h3>
                <time dateTime={event.startAt}>
                  {formatEventTime(event.startAt, event.timezone)}
                </time>
                {event.description ? <p>{event.description}</p> : null}
              </article>
            ))}
          </div>
        </section>
      ) : null}

      {payload.locations.length ? (
        <section className="render-block">
          <h2>Địa điểm</h2>
          <div className="render-list">
            {payload.locations.map((location) => (
              <article className="render-card" key={location.id}>
                <h3>{location.name}</h3>
                {location.address ? <p>{location.address}</p> : null}
                {location.mapUrl ? (
                  <a href={location.mapUrl} target="_blank" rel="noreferrer">
                    Xem bản đồ
                  </a>
                ) : null}
              </article>
            ))}
          </div>
        </section>
      ) : null}
    </article>
  );
}

function ImageGallery({
  images,
}: {
  images: TemplateRendererProps["payload"]["images"];
}) {
  if (!images.length) {
    return null;
  }
  return (
    <div className="render-gallery">
      {images.map((image) => (
        <figure key={image.id}>
          <img
            src={image.asset.deliveryUrl}
            alt={image.altText ?? image.caption ?? "Ảnh kỷ niệm"}
          />
          {image.caption ? <figcaption>{image.caption}</figcaption> : null}
        </figure>
      ))}
    </div>
  );
}

function formatEventTime(value: string, timezone: string) {
  try {
    return new Intl.DateTimeFormat("vi-VN", {
      dateStyle: "long",
      timeStyle: "short",
      timeZone: timezone,
    }).format(new Date(value));
  } catch {
    return new Date(value).toLocaleString("vi-VN");
  }
}

function stringConfig(
  config: Record<string, unknown>,
  key: string,
  fallback: string,
) {
  const value = config[key];
  return typeof value === "string" && value.trim() ? value : fallback;
}
