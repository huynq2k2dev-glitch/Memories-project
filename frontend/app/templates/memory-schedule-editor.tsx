"use client";

import { type FormEvent, useCallback, useEffect, useState } from "react";

import { authenticatedFetch } from "@/lib/auth-session";

type MemoryLocation = {
  id: string;
  name: string;
  address: string | null;
  latitude: number | null;
  longitude: number | null;
  mapUrl: string | null;
  note: string | null;
  sortOrder: number;
  version: number;
};

type MemoryEvent = {
  id: string;
  locationId: string | null;
  eventType: string;
  title: string;
  description: string | null;
  startAt: string;
  endAt: string | null;
  timezone: string;
  sortOrder: number;
  rsvpEnabled: boolean;
  version: number;
};

type VersionedItem = {
  id: string;
  version: number;
};

class ScheduleProblem extends Error {
  constructor(
    message: string,
    readonly code?: string,
  ) {
    super(message);
  }
}

export default function MemoryScheduleEditor({ memoryId }: { memoryId: string }) {
  const [locations, setLocations] = useState<MemoryLocation[]>([]);
  const [events, setEvents] = useState<MemoryEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setBusy(true);
    setError("");
    try {
      const [loadedLocations, loadedEvents] = await Promise.all([
        requestJson<MemoryLocation[]>(`/api/memories/${memoryId}/locations`),
        requestJson<MemoryEvent[]>(`/api/memories/${memoryId}/events`),
      ]);
      setLocations(loadedLocations);
      setEvents(loadedEvents);
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }, [memoryId]);

  useEffect(() => {
    let active = true;
    void Promise.all([
      requestJson<MemoryLocation[]>(`/api/memories/${memoryId}/locations`),
      requestJson<MemoryEvent[]>(`/api/memories/${memoryId}/events`),
    ])
      .then(([loadedLocations, loadedEvents]) => {
        if (active) {
          setLocations(loadedLocations);
          setEvents(loadedEvents);
        }
      })
      .catch((reason: unknown) => {
        if (active) {
          setError(errorMessage(reason));
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, [memoryId]);

  async function moveLocation(index: number, direction: -1 | 1) {
    const reordered = move(locations, index, direction);
    if (!reordered) {
      return;
    }
    await runOrderMutation(
      `/api/memories/${memoryId}/locations/order`,
      reordered,
      setLocations,
    );
  }

  async function moveEvent(index: number, direction: -1 | 1) {
    const reordered = move(events, index, direction);
    if (!reordered) {
      return;
    }
    await runOrderMutation(
      `/api/memories/${memoryId}/events/order`,
      reordered,
      setEvents,
    );
  }

  async function runOrderMutation<T extends VersionedItem>(
    path: string,
    items: T[],
    update: (items: T[]) => void,
  ) {
    setBusy(true);
    setError("");
    try {
      update(await reorderItems<T>(path, items));
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  function locationDeleted(locationId: string) {
    setLocations((current) => current.filter((location) => location.id !== locationId));
    setEvents((current) =>
      current.map((event) =>
        event.locationId === locationId ? { ...event, locationId: null } : event,
      ),
    );
  }

  return (
    <section className="memory-content-editor" aria-busy={loading || busy}>
      <header>
        <div>
          <h3>Địa điểm và sự kiện</h3>
          <p className="form-note">
            Instant được nhập bằng ISO-8601 có offset; timezone chỉ dùng để hiển thị
            lịch địa phương.
          </p>
        </div>
        <button type="button" disabled={loading || busy} onClick={() => void load()}>
          Tải lại lịch
        </button>
      </header>

      {error ? (
        <p className="form-note form-error" role="alert">
          {error}
        </p>
      ) : null}
      {loading ? <p>Đang tải địa điểm và sự kiện…</p> : null}

      {!loading ? (
        <div className="content-editor-columns">
          <section>
            <h4>Địa điểm</h4>
            <CreateLocationForm
              memoryId={memoryId}
              sortOrder={nextSortOrder(locations)}
              onCreated={(location) =>
                setLocations((current) => [...current, location])
              }
            />
            <div className="content-item-list">
              {locations.map((location, index) => (
                <LocationCard
                  key={`${location.id}-${location.version}`}
                  memoryId={memoryId}
                  location={location}
                  first={index === 0}
                  last={index === locations.length - 1}
                  onMove={(direction) => void moveLocation(index, direction)}
                  onUpdated={(updated) =>
                    setLocations((current) => replaceItem(current, updated))
                  }
                  onDeleted={() => locationDeleted(location.id)}
                  onReload={load}
                />
              ))}
            </div>
          </section>

          <section>
            <h4>Sự kiện</h4>
            <CreateEventForm
              memoryId={memoryId}
              locations={locations}
              sortOrder={nextSortOrder(events)}
              onCreated={(event) => setEvents((current) => [...current, event])}
            />
            <div className="content-item-list">
              {events.map((event, index) => (
                <EventCard
                  key={`${event.id}-${event.version}`}
                  memoryId={memoryId}
                  event={event}
                  locations={locations}
                  first={index === 0}
                  last={index === events.length - 1}
                  onMove={(direction) => void moveEvent(index, direction)}
                  onUpdated={(updated) =>
                    setEvents((current) => replaceItem(current, updated))
                  }
                  onDeleted={() =>
                    setEvents((current) =>
                      current.filter((item) => item.id !== event.id),
                    )
                  }
                  onReload={load}
                />
              ))}
            </div>
          </section>
        </div>
      ) : null}
    </section>
  );
}

function CreateLocationForm({
  memoryId,
  sortOrder,
  onCreated,
}: {
  memoryId: string;
  sortOrder: number;
  onCreated: (location: MemoryLocation) => void;
}) {
  const [name, setName] = useState("");
  const [address, setAddress] = useState("");
  const [latitude, setLatitude] = useState("");
  const [longitude, setLongitude] = useState("");
  const [mapUrl, setMapUrl] = useState("");
  const [note, setNote] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const coordinates = parseCoordinates(latitude, longitude);
      const location = await requestJson<MemoryLocation>(
        `/api/memories/${memoryId}/locations`,
        "POST",
        { name, address, ...coordinates, mapUrl, note, sortOrder },
      );
      onCreated(location);
      setName("");
      setAddress("");
      setLatitude("");
      setLongitude("");
      setMapUrl("");
      setNote("");
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="content-item-form" onSubmit={submit}>
      <LocationFields
        prefix="new-location"
        name={name}
        address={address}
        latitude={latitude}
        longitude={longitude}
        mapUrl={mapUrl}
        note={note}
        setName={setName}
        setAddress={setAddress}
        setLatitude={setLatitude}
        setLongitude={setLongitude}
        setMapUrl={setMapUrl}
        setNote={setNote}
      />
      <button type="submit" disabled={busy}>
        {busy ? "Đang thêm…" : "Thêm địa điểm"}
      </button>
      <ScheduleError error={error} />
    </form>
  );
}

function LocationCard({
  memoryId,
  location,
  first,
  last,
  onMove,
  onUpdated,
  onDeleted,
  onReload,
}: {
  memoryId: string;
  location: MemoryLocation;
  first: boolean;
  last: boolean;
  onMove: (direction: -1 | 1) => void;
  onUpdated: (location: MemoryLocation) => void;
  onDeleted: () => void;
  onReload: () => Promise<void>;
}) {
  const [name, setName] = useState(location.name);
  const [address, setAddress] = useState(location.address ?? "");
  const [latitude, setLatitude] = useState(toInputNumber(location.latitude));
  const [longitude, setLongitude] = useState(toInputNumber(location.longitude));
  const [mapUrl, setMapUrl] = useState(location.mapUrl ?? "");
  const [note, setNote] = useState(location.note ?? "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [conflict, setConflict] = useState(false);

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setConflict(false);
    try {
      onUpdated(
        await requestJson<MemoryLocation>(
          `/api/memories/${memoryId}/locations/${location.id}`,
          "PUT",
          {
            name,
            address,
            ...parseCoordinates(latitude, longitude),
            mapUrl,
            note,
            version: location.version,
          },
        ),
      );
    } catch (reason) {
      setConflict(isConflict(reason));
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  async function remove() {
    if (!window.confirm(`Xóa địa điểm “${location.name}”?`)) {
      return;
    }
    setBusy(true);
    setError("");
    try {
      await requestJson<void>(
        `/api/memories/${memoryId}/locations/${location.id}?version=${location.version}`,
        "DELETE",
      );
      onDeleted();
    } catch (reason) {
      setConflict(isConflict(reason));
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="content-item-card" onSubmit={save}>
      <div className="content-item-heading">
        <strong>{location.name}</strong>
        <span>#{location.sortOrder}</span>
      </div>
      <LocationFields
        prefix={`location-${location.id}`}
        name={name}
        address={address}
        latitude={latitude}
        longitude={longitude}
        mapUrl={mapUrl}
        note={note}
        setName={setName}
        setAddress={setAddress}
        setLatitude={setLatitude}
        setLongitude={setLongitude}
        setMapUrl={setMapUrl}
        setNote={setNote}
      />
      <ScheduleActions
        busy={busy}
        first={first}
        last={last}
        onMove={onMove}
        onDelete={() => void remove()}
      />
      <ScheduleError error={error} conflict={conflict} onReload={onReload} />
    </form>
  );
}

function LocationFields({
  prefix,
  name,
  address,
  latitude,
  longitude,
  mapUrl,
  note,
  setName,
  setAddress,
  setLatitude,
  setLongitude,
  setMapUrl,
  setNote,
}: {
  prefix: string;
  name: string;
  address: string;
  latitude: string;
  longitude: string;
  mapUrl: string;
  note: string;
  setName: (value: string) => void;
  setAddress: (value: string) => void;
  setLatitude: (value: string) => void;
  setLongitude: (value: string) => void;
  setMapUrl: (value: string) => void;
  setNote: (value: string) => void;
}) {
  return (
    <>
      <label htmlFor={`${prefix}-name`}>Tên địa điểm</label>
      <input
        id={`${prefix}-name`}
        value={name}
        onChange={(event) => setName(event.target.value)}
        maxLength={200}
        required
      />
      <label htmlFor={`${prefix}-address`}>Địa chỉ</label>
      <input
        id={`${prefix}-address`}
        value={address}
        onChange={(event) => setAddress(event.target.value)}
        maxLength={500}
      />
      <div className="coordinate-fields">
        <div>
          <label htmlFor={`${prefix}-latitude`}>Latitude</label>
          <input
            id={`${prefix}-latitude`}
            type="number"
            min="-90"
            max="90"
            step="0.0000001"
            value={latitude}
            onChange={(event) => setLatitude(event.target.value)}
          />
        </div>
        <div>
          <label htmlFor={`${prefix}-longitude`}>Longitude</label>
          <input
            id={`${prefix}-longitude`}
            type="number"
            min="-180"
            max="180"
            step="0.0000001"
            value={longitude}
            onChange={(event) => setLongitude(event.target.value)}
          />
        </div>
      </div>
      <label htmlFor={`${prefix}-map-url`}>Map URL HTTPS</label>
      <input
        id={`${prefix}-map-url`}
        type="url"
        value={mapUrl}
        onChange={(event) => setMapUrl(event.target.value)}
        maxLength={2048}
        placeholder="https://www.google.com/maps/..."
      />
      <label htmlFor={`${prefix}-note`}>Ghi chú</label>
      <textarea
        id={`${prefix}-note`}
        value={note}
        onChange={(event) => setNote(event.target.value)}
        maxLength={1000}
        rows={3}
      />
    </>
  );
}

function CreateEventForm({
  memoryId,
  locations,
  sortOrder,
  onCreated,
}: {
  memoryId: string;
  locations: MemoryLocation[];
  sortOrder: number;
  onCreated: (event: MemoryEvent) => void;
}) {
  const [locationId, setLocationId] = useState("");
  const [eventType, setEventType] = useState("MILESTONE");
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [startAt, setStartAt] = useState("");
  const [endAt, setEndAt] = useState("");
  const [timezone, setTimezone] = useState("Asia/Ho_Chi_Minh");
  const [rsvpEnabled, setRsvpEnabled] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const created = await requestJson<MemoryEvent>(
        `/api/memories/${memoryId}/events`,
        "POST",
        {
          locationId: locationId || null,
          eventType,
          title,
          description,
          startAt: parseInstant(startAt),
          endAt: endAt ? parseInstant(endAt) : null,
          timezone,
          sortOrder,
          rsvpEnabled,
        },
      );
      onCreated(created);
      setTitle("");
      setDescription("");
      setStartAt("");
      setEndAt("");
      setRsvpEnabled(false);
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="content-item-form" onSubmit={submit}>
      <EventFields
        prefix="new-event"
        locations={locations}
        locationId={locationId}
        eventType={eventType}
        title={title}
        description={description}
        startAt={startAt}
        endAt={endAt}
        timezone={timezone}
        rsvpEnabled={rsvpEnabled}
        setLocationId={setLocationId}
        setEventType={setEventType}
        setTitle={setTitle}
        setDescription={setDescription}
        setStartAt={setStartAt}
        setEndAt={setEndAt}
        setTimezone={setTimezone}
        setRsvpEnabled={setRsvpEnabled}
      />
      <button type="submit" disabled={busy}>
        {busy ? "Đang thêm…" : "Thêm sự kiện"}
      </button>
      <ScheduleError error={error} />
    </form>
  );
}

function EventCard({
  memoryId,
  event,
  locations,
  first,
  last,
  onMove,
  onUpdated,
  onDeleted,
  onReload,
}: {
  memoryId: string;
  event: MemoryEvent;
  locations: MemoryLocation[];
  first: boolean;
  last: boolean;
  onMove: (direction: -1 | 1) => void;
  onUpdated: (event: MemoryEvent) => void;
  onDeleted: () => void;
  onReload: () => Promise<void>;
}) {
  const [locationId, setLocationId] = useState(event.locationId ?? "");
  const [eventType, setEventType] = useState(event.eventType);
  const [title, setTitle] = useState(event.title);
  const [description, setDescription] = useState(event.description ?? "");
  const [startAt, setStartAt] = useState(event.startAt);
  const [endAt, setEndAt] = useState(event.endAt ?? "");
  const [timezone, setTimezone] = useState(event.timezone);
  const [rsvpEnabled, setRsvpEnabled] = useState(event.rsvpEnabled);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [conflict, setConflict] = useState(false);

  async function save(formEvent: FormEvent<HTMLFormElement>) {
    formEvent.preventDefault();
    setBusy(true);
    setError("");
    setConflict(false);
    try {
      onUpdated(
        await requestJson<MemoryEvent>(
          `/api/memories/${memoryId}/events/${event.id}`,
          "PUT",
          {
            locationId: locationId || null,
            eventType,
            title,
            description,
            startAt: parseInstant(startAt),
            endAt: endAt ? parseInstant(endAt) : null,
            timezone,
            rsvpEnabled,
            version: event.version,
          },
        ),
      );
    } catch (reason) {
      setConflict(isConflict(reason));
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  async function remove() {
    if (!window.confirm(`Xóa sự kiện “${event.title}”?`)) {
      return;
    }
    setBusy(true);
    setError("");
    try {
      await requestJson<void>(
        `/api/memories/${memoryId}/events/${event.id}?version=${event.version}`,
        "DELETE",
      );
      onDeleted();
    } catch (reason) {
      setConflict(isConflict(reason));
      setError(errorMessage(reason));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="content-item-card" onSubmit={save}>
      <div className="content-item-heading">
        <strong>{event.title}</strong>
        <span>
          #{event.sortOrder} · {formatEventTime(event.startAt, event.timezone)}
        </span>
      </div>
      <EventFields
        prefix={`event-${event.id}`}
        locations={locations}
        locationId={locationId}
        eventType={eventType}
        title={title}
        description={description}
        startAt={startAt}
        endAt={endAt}
        timezone={timezone}
        rsvpEnabled={rsvpEnabled}
        setLocationId={setLocationId}
        setEventType={setEventType}
        setTitle={setTitle}
        setDescription={setDescription}
        setStartAt={setStartAt}
        setEndAt={setEndAt}
        setTimezone={setTimezone}
        setRsvpEnabled={setRsvpEnabled}
      />
      <ScheduleActions
        busy={busy}
        first={first}
        last={last}
        onMove={onMove}
        onDelete={() => void remove()}
      />
      <ScheduleError error={error} conflict={conflict} onReload={onReload} />
    </form>
  );
}

type EventFieldProps = {
  prefix: string;
  locations: MemoryLocation[];
  locationId: string;
  eventType: string;
  title: string;
  description: string;
  startAt: string;
  endAt: string;
  timezone: string;
  rsvpEnabled: boolean;
  setLocationId: (value: string) => void;
  setEventType: (value: string) => void;
  setTitle: (value: string) => void;
  setDescription: (value: string) => void;
  setStartAt: (value: string) => void;
  setEndAt: (value: string) => void;
  setTimezone: (value: string) => void;
  setRsvpEnabled: (value: boolean) => void;
};

function EventFields(props: EventFieldProps) {
  return (
    <>
      <label htmlFor={`${props.prefix}-type`}>Event type</label>
      <input
        id={`${props.prefix}-type`}
        value={props.eventType}
        onChange={(event) => props.setEventType(event.target.value.toUpperCase())}
        pattern="[A-Z][A-Z0-9_]{0,49}"
        maxLength={50}
        required
      />
      <label htmlFor={`${props.prefix}-title`}>Tên sự kiện</label>
      <input
        id={`${props.prefix}-title`}
        value={props.title}
        onChange={(event) => props.setTitle(event.target.value)}
        maxLength={255}
        required
      />
      <label htmlFor={`${props.prefix}-location`}>Địa điểm</label>
      <select
        id={`${props.prefix}-location`}
        value={props.locationId}
        onChange={(event) => props.setLocationId(event.target.value)}
      >
        <option value="">Không gắn địa điểm</option>
        {props.locations.map((location) => (
          <option key={location.id} value={location.id}>
            {location.name}
          </option>
        ))}
      </select>
      <label htmlFor={`${props.prefix}-description`}>Mô tả</label>
      <textarea
        id={`${props.prefix}-description`}
        value={props.description}
        onChange={(event) => props.setDescription(event.target.value)}
        rows={4}
      />
      <label htmlFor={`${props.prefix}-start`}>Bắt đầu (ISO-8601 Instant)</label>
      <input
        id={`${props.prefix}-start`}
        value={props.startAt}
        onChange={(event) => props.setStartAt(event.target.value)}
        placeholder="2026-08-22T10:00:00+07:00"
        required
      />
      <label htmlFor={`${props.prefix}-end`}>Kết thúc (ISO-8601 Instant)</label>
      <input
        id={`${props.prefix}-end`}
        value={props.endAt}
        onChange={(event) => props.setEndAt(event.target.value)}
        placeholder="2026-08-22T12:00:00+07:00"
      />
      <label htmlFor={`${props.prefix}-timezone`}>IANA timezone hiển thị</label>
      <input
        id={`${props.prefix}-timezone`}
        value={props.timezone}
        onChange={(event) => props.setTimezone(event.target.value)}
        maxLength={50}
        placeholder="Asia/Ho_Chi_Minh"
      />
      <label className="checkbox-label" htmlFor={`${props.prefix}-rsvp`}>
        <input
          id={`${props.prefix}-rsvp`}
          type="checkbox"
          checked={props.rsvpEnabled}
          onChange={(event) => props.setRsvpEnabled(event.target.checked)}
        />
        Cho phép RSVP
      </label>
    </>
  );
}

function ScheduleActions({
  busy,
  first,
  last,
  onMove,
  onDelete,
}: {
  busy: boolean;
  first: boolean;
  last: boolean;
  onMove: (direction: -1 | 1) => void;
  onDelete: () => void;
}) {
  return (
    <div className="content-item-actions">
      <button type="submit" disabled={busy}>
        {busy ? "Đang xử lý…" : "Lưu"}
      </button>
      <button type="button" disabled={busy || first} onClick={() => onMove(-1)}>
        Lên
      </button>
      <button type="button" disabled={busy || last} onClick={() => onMove(1)}>
        Xuống
      </button>
      <button className="danger-button" type="button" disabled={busy} onClick={onDelete}>
        Xóa
      </button>
    </div>
  );
}

function ScheduleError({
  error,
  conflict = false,
  onReload,
}: {
  error: string;
  conflict?: boolean;
  onReload?: () => Promise<void>;
}) {
  if (!error) {
    return null;
  }
  return (
    <div className="form-note form-error" role="alert">
      <p>{error}</p>
      {conflict && onReload ? (
        <button type="button" onClick={() => void onReload()}>
          Tải lại bản hiện hành
        </button>
      ) : null}
    </div>
  );
}

async function requestJson<T>(
  path: string,
  method: "GET" | "POST" | "PUT" | "DELETE" = "GET",
  body?: unknown,
): Promise<T> {
  const response = await authenticatedFetch(path, {
    method,
    ...(body === undefined
      ? {}
      : {
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
        }),
    cache: "no-store",
  });
  if (!response.ok) {
    const problem = await readProblem(response);
    throw new ScheduleProblem(problem.detail, problem.code);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

async function reorderItems<T extends VersionedItem>(path: string, items: T[]) {
  return requestJson<T[]>(path, "PUT", {
    orderedIds: items.map((item) => item.id),
    versions: Object.fromEntries(items.map((item) => [item.id, item.version])),
  });
}

async function readProblem(response: Response) {
  try {
    const problem = (await response.json()) as { detail?: string; code?: string };
    return {
      code: problem.code,
      detail: problem.detail ?? "Không thể cập nhật lịch memory.",
    };
  } catch {
    return { detail: "Không thể cập nhật lịch memory." };
  }
}

function parseCoordinates(latitude: string, longitude: string) {
  if (!latitude.trim() && !longitude.trim()) {
    return { latitude: null, longitude: null };
  }
  if (!latitude.trim() || !longitude.trim()) {
    throw new Error("Latitude và longitude phải được nhập cùng nhau.");
  }
  const parsedLatitude = Number(latitude);
  const parsedLongitude = Number(longitude);
  if (!Number.isFinite(parsedLatitude) || !Number.isFinite(parsedLongitude)) {
    throw new Error("Tọa độ không hợp lệ.");
  }
  return { latitude: parsedLatitude, longitude: parsedLongitude };
}

function parseInstant(value: string) {
  if (!/(?:Z|[+-]\d{2}:\d{2})$/i.test(value.trim())) {
    throw new Error("Thời gian phải có Z hoặc UTC offset rõ ràng.");
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    throw new Error("Thời gian ISO-8601 không hợp lệ.");
  }
  return date.toISOString();
}

function formatEventTime(value: string, timezone: string) {
  try {
    return new Intl.DateTimeFormat("vi-VN", {
      dateStyle: "medium",
      timeStyle: "short",
      timeZone: timezone,
    }).format(new Date(value));
  } catch {
    return value;
  }
}

function move<T>(items: T[], index: number, direction: -1 | 1) {
  const target = index + direction;
  if (target < 0 || target >= items.length) {
    return null;
  }
  const reordered = [...items];
  [reordered[index], reordered[target]] = [reordered[target], reordered[index]];
  return reordered;
}

function replaceItem<T extends { id: string }>(items: T[], updated: T) {
  return items.map((item) => (item.id === updated.id ? updated : item));
}

function nextSortOrder(items: Array<{ sortOrder: number }>) {
  return items.reduce((maximum, item) => Math.max(maximum, item.sortOrder), -1) + 1;
}

function toInputNumber(value: number | null) {
  return value === null ? "" : value.toString();
}

function isConflict(reason: unknown) {
  return reason instanceof ScheduleProblem && reason.code === "MEMORY_VERSION_CONFLICT";
}

function errorMessage(reason: unknown) {
  return reason instanceof Error ? reason.message : "Dịch vụ tạm thời không khả dụng.";
}
