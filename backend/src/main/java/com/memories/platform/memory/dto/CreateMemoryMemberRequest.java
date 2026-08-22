package com.memories.platform.memory.dto;

import com.memories.platform.memory.constants.MemoryContentConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record CreateMemoryMemberRequest(
        @NotBlank
        @Pattern(regexp = MemoryContentConstants.STRUCTURED_CODE_PATTERN)
        String roleCode,
        @NotBlank @Size(max = 200) String fullName,
        @Size(max = 150) String displayName,
        String description,
        @NotNull @PositiveOrZero Integer sortOrder
) {
    public CreateMemoryMemberRequest {
        roleCode = normalizeCode(roleCode);
        fullName = normalizeRequired(fullName);
        displayName = normalizeOptional(displayName);
        description = normalizeOptional(description);
    }

    private static String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
