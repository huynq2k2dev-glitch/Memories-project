package com.memories.platform.memory.dto;

import com.memories.platform.common.domain.MemoryType;
import com.memories.platform.memory.entity.MemoryStatus;

import java.time.Instant;
import java.util.UUID;

public record MemorySummaryResponse(
        UUID id,
        String title,
        MemoryType memoryType,
        MemoryStatus status,
        String slug,
        MemorySummaryCoverResponse cover,
        Instant updatedAt
) {
}
