package com.memories.platform.memory.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GuestMemoryContextResponse(
        String title,
        List<Event> events
) {
    public record Event(
            UUID id,
            String eventType,
            String title,
            String description,
            Instant startAt,
            Instant endAt,
            String timezone,
            int sortOrder
    ) {
    }
}
