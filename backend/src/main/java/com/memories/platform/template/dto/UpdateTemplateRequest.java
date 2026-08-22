package com.memories.platform.template.dto;

import com.memories.platform.common.domain.MemoryType;
import com.memories.platform.template.entity.TemplateStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTemplateRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull MemoryType memoryType,
        @Size(max = 1000) String description,
        @NotNull TemplateStatus status
) {
    public UpdateTemplateRequest {
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
