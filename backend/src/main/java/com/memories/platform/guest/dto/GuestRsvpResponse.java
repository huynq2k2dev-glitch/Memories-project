package com.memories.platform.guest.dto;

import com.memories.platform.guest.entity.GuestAttendanceStatus;

import java.time.Instant;
import java.util.UUID;

public record GuestRsvpResponse(
        UUID eventId,
        GuestAttendanceStatus attendanceStatus,
        int partySize,
        String dietaryNote,
        String message,
        Instant respondedAt,
        Instant updatedAt,
        long version
) {
}
