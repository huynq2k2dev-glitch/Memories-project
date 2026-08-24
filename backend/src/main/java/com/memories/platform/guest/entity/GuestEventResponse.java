package com.memories.platform.guest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "guest_event_responses")
public class GuestEventResponse {

    @Id
    private UUID id;

    @Column(name = "guest_id", nullable = false, updatable = false)
    private UUID guestId;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false, length = 20)
    private GuestAttendanceStatus attendanceStatus;

    @Column(name = "party_size", nullable = false)
    private int partySize;

    @Column(name = "dietary_note", length = 500)
    private String dietaryNote;

    @Column(length = 1000)
    private String message;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected GuestEventResponse() {
    }

    public GuestEventResponse(
            UUID id,
            UUID guestId,
            UUID eventId,
            GuestAttendanceStatus attendanceStatus,
            int partySize,
            String dietaryNote,
            String message,
            Instant respondedAt,
            Instant now
    ) {
        this.id = id;
        this.guestId = guestId;
        this.eventId = eventId;
        this.attendanceStatus = attendanceStatus;
        this.partySize = partySize;
        this.dietaryNote = dietaryNote;
        this.message = message;
        this.respondedAt = respondedAt;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(
            GuestAttendanceStatus attendanceStatus,
            int partySize,
            String dietaryNote,
            String message,
            Instant respondedAt,
            Instant now
    ) {
        this.attendanceStatus = attendanceStatus;
        this.partySize = partySize;
        this.dietaryNote = dietaryNote;
        this.message = message;
        this.respondedAt = respondedAt;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getGuestId() {
        return guestId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public GuestAttendanceStatus getAttendanceStatus() {
        return attendanceStatus;
    }

    public int getPartySize() {
        return partySize;
    }

    public String getDietaryNote() {
        return dietaryNote;
    }

    public String getMessage() {
        return message;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
