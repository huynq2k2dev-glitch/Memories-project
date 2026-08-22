package com.memories.platform.memory.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.memories.platform.memory.constants.MemoryContentConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record CreateMemorySectionRequest(
        @NotBlank @Size(max = 100) String sectionKey,
        @NotBlank
        @Pattern(regexp = MemoryContentConstants.STRUCTURED_CODE_PATTERN)
        String sectionType,
        @Size(max = 255) String title,
        String contentText,
        @NotNull JsonNode config,
        @NotNull @PositiveOrZero Integer sortOrder,
        @NotNull Boolean visible
) {
    public CreateMemorySectionRequest {
        sectionKey = normalizeRequired(sectionKey);
        sectionType = sectionType == null
                ? null
                : sectionType.trim().toUpperCase(Locale.ROOT);
        title = normalizeOptional(title);
        contentText = normalizeOptional(contentText);
    }

    private static String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
