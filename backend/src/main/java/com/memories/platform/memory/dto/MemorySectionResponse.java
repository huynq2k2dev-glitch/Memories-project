package com.memories.platform.memory.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record MemorySectionResponse(
        UUID id,
        String sectionKey,
        String sectionType,
        String title,
        String contentText,
        JsonNode config,
        int sortOrder,
        boolean visible,
        boolean required,
        boolean contentComplete,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
}
