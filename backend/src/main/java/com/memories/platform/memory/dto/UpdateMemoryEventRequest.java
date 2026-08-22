package com.memories.platform.memory.dto;

import com.memories.platform.memory.constants.MemoryContentConstants;
import com.memories.platform.memory.constants.MemoryScheduleConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public record UpdateMemoryEventRequest(
        UUID locationId,
        @NotBlank
        @Pattern(regexp = MemoryContentConstants.STRUCTURED_CODE_PATTERN)
        String eventType,
        @NotBlank @Size(max = 255) String title,
        String description,
        @NotNull Instant startAt,
        Instant endAt,
        @Size(max = 50) String timezone,
        @NotNull Boolean rsvpEnabled,
        @NotNull @PositiveOrZero Long version
) {
    public UpdateMemoryEventRequest {
        eventType = eventType == null
                ? null
                : eventType.trim().toUpperCase(Locale.ROOT);
        title = normalizeRequired(title);
        description = normalizeOptional(description);
        timezone = timezone == null || timezone.isBlank()
                ? MemoryScheduleConstants.DEFAULT_TIMEZONE
                : timezone.trim();
    }

    private static String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
