package com.memories.platform.memory.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.memories.platform.memory.entity.MemoryVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpdateMemoryRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 1000) String summary,
        @NotNull MemoryVisibility visibility,
        @NotNull JsonNode themeConfig,
        Instant eventStartAt,
        Instant expiresAt,
        @NotNull @PositiveOrZero Long version
) {
    public UpdateMemoryRequest {
        title = title == null ? null : title.trim();
        summary = normalizeOptional(summary);
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
