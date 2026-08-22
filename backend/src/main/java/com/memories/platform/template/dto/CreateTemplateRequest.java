package com.memories.platform.template.dto;

import com.memories.platform.common.domain.MemoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTemplateRequest(
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "[A-Z][A-Z0-9_]{2,99}")
        String code,
        @NotBlank @Size(max = 150) String name,
        @NotNull MemoryType memoryType,
        @Size(max = 1000) String description
) {
    public CreateTemplateRequest {
        code = code == null ? null : code.trim();
        name = name == null ? null : name.trim();
        description = normalizeOptional(description);
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
