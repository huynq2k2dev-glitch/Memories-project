package com.memories.platform.memory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MemoryLocationResponse(
        UUID id,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String mapUrl,
        String note,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
}
