package com.memories.platform.memory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateMemoryAssetReferenceRequest(
        UUID assetId,
        @NotNull @Min(0) Long version
) {
}
