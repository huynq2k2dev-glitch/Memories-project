package com.memories.platform.memory.dto;

import com.memories.platform.memory.entity.MemoryStatus;

import java.time.Instant;
import java.util.UUID;

public record MemoryLifecycleResponse(
        UUID id,
        MemoryStatus status,
        Instant updatedAt,
        long version
) {
}
