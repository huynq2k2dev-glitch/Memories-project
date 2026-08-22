package com.memories.platform.memory.dto;

import com.memories.platform.memory.entity.MemoryStatus;
import com.memories.platform.memory.entity.MemoryVisibility;

import java.time.Instant;
import java.util.UUID;

public record PublishMemoryResponse(
        UUID id,
        String slug,
        MemoryStatus status,
        MemoryVisibility visibility,
        Instant publishedAt,
        long version
) {
}
