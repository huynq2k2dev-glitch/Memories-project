package com.memories.platform.memory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "memory_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemoryEvent {

    @Id
    private UUID id;

    @Column(name = "memory_id", nullable = false, updatable = false)
    private UUID memoryId;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;

    @Column(nullable = false, length = 50)
    private String timezone;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "rsvp_enabled", nullable = false)
    private boolean rsvpEnabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Version
    @Column(nullable = false)
    private long version;

    public MemoryEvent(
            UUID id,
            UUID memoryId,
            UUID locationId,
            String eventType,
            String title,
            String description,
            Instant startAt,
            Instant endAt,
            String timezone,
            int sortOrder,
            boolean rsvpEnabled,
            UUID actorId,
            Instant now
    ) {
        this.id = id;
        this.memoryId = memoryId;
        this.locationId = locationId;
        this.eventType = eventType;
        this.title = title;
        this.description = description;
        this.startAt = startAt;
        this.endAt = endAt;
        this.timezone = timezone;
        this.sortOrder = sortOrder;
        this.rsvpEnabled = rsvpEnabled;
        this.createdAt = now;
        this.createdBy = actorId;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public void update(
            UUID locationId,
            String eventType,
            String title,
            String description,
            Instant startAt,
            Instant endAt,
            String timezone,
            boolean rsvpEnabled,
            UUID actorId,
            Instant now
    ) {
        this.locationId = locationId;
        this.eventType = eventType;
        this.title = title;
        this.description = description;
        this.startAt = startAt;
        this.endAt = endAt;
        this.timezone = timezone;
        this.rsvpEnabled = rsvpEnabled;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public void reorder(int sortOrder, UUID actorId, Instant now) {
        this.sortOrder = sortOrder;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }
}
