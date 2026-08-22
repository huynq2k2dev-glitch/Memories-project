package com.memories.platform.memory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ReorderMemoryItemsRequest(
        @NotNull List<@NotNull UUID> orderedIds,
        @NotNull Map<@NotNull UUID, @NotNull @PositiveOrZero Long> versions
) {
    public ReorderMemoryItemsRequest {
        orderedIds = orderedIds == null ? null : List.copyOf(orderedIds);
        versions = versions == null ? null : Map.copyOf(versions);
    }
}
