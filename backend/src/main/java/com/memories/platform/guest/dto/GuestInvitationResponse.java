package com.memories.platform.guest.dto;

import com.memories.platform.guest.entity.GuestAttendanceStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GuestInvitationResponse(
        Guest guest,
        Memory memory,
        List<Event> events
) {
    public record Guest(
            String fullName,
            String guestGroup,
            int maxPartySize
    ) {
    }

    public record Memory(String title) {
    }

    public record Event(
            UUID id,
            String eventType,
            String title,
            String description,
            Instant startAt,
            Instant endAt,
            String timezone,
            int sortOrder,
            Rsvp rsvp
    ) {
    }

    public record Rsvp(
            GuestAttendanceStatus attendanceStatus,
            int partySize,
            String dietaryNote,
            String message,
            Instant respondedAt,
            Instant updatedAt,
            long version
    ) {
    }
}
