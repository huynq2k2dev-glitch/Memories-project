package com.memories.platform.memory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateMemoryImageRequest(
        UUID sectionId,
        @Size(max = 1000) String caption,
        @Size(max = 500) String altText,
        boolean coverCandidate,
        @NotNull @Min(0) Long version
) {
}
