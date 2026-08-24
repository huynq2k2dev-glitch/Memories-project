package com.memories.platform.guest.dto;

import com.memories.platform.guest.entity.GuestAttendanceStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SubmitGuestRsvpRequest(
        @NotNull UUID eventId,
        @NotNull GuestAttendanceStatus attendanceStatus,
        @NotNull @Min(0) @Max(50) Integer partySize,
        @Size(max = 500) String dietaryNote,
        @Size(max = 1000) String message,
        @PositiveOrZero Long version
) {
}
