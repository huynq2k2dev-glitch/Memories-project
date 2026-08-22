package com.memories.platform.memory.dto;

import com.memories.platform.common.domain.MemoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateMemoryRequest(
        @NotNull UUID templateVersionId,
        @NotNull MemoryType memoryType,
        @NotBlank @Size(max = 255) String title
) {
    public CreateMemoryRequest {
        title = title == null ? null : title.trim();
    }
}
