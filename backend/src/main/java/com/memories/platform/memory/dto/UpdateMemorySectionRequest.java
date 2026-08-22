package com.memories.platform.memory.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.memories.platform.memory.constants.MemoryContentConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record UpdateMemorySectionRequest(
        @NotBlank
        @Pattern(regexp = MemoryContentConstants.STRUCTURED_CODE_PATTERN)
        String sectionType,
        @Size(max = 255) String title,
        String contentText,
        @NotNull JsonNode config,
        @NotNull Boolean visible,
        @NotNull @PositiveOrZero Long version
) {
    public UpdateMemorySectionRequest {
        sectionType = sectionType == null
                ? null
                : sectionType.trim().toUpperCase(Locale.ROOT);
        title = normalizeOptional(title);
        contentText = normalizeOptional(contentText);
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
