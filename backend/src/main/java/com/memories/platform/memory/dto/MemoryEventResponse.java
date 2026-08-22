package com.memories.platform.memory.dto;

import java.time.Instant;
import java.util.UUID;

public record MemoryEventResponse(
        UUID id,
        UUID locationId,
        String eventType,
        String title,
        String description,
        Instant startAt,
        Instant endAt,
        String timezone,
        int sortOrder,
        boolean rsvpEnabled,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
}
