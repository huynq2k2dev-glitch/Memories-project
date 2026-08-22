package com.memories.platform.memory.dto;

import java.time.Instant;
import java.util.UUID;

public record MemoryImageResponse(
        UUID id,
        UUID assetId,
        UUID sectionId,
        String caption,
        String altText,
        int sortOrder,
        boolean coverCandidate,
        String deliveryUrl,
        long assetVersion,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
}
