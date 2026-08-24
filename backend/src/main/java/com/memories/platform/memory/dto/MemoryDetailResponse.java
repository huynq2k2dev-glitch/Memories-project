package com.memories.platform.memory.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.memories.platform.common.domain.MemoryType;
import com.memories.platform.memory.entity.MemoryStatus;
import com.memories.platform.memory.entity.MemoryVisibility;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MemoryDetailResponse(
        UUID id,
        UUID ownerId,
        UUID templateVersionId,
        String slug,
        String title,
        MemoryType memoryType,
        MemoryStatus status,
        MemoryVisibility visibility,
        String summary,
        JsonNode themeConfig,
        JsonNode settings,
        UUID coverAssetId,
        Instant eventStartAt,
        Instant publishedAt,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt,
        long version,
        List<String> allowedSectionTypes,
        MemoryCapabilitiesResponse capabilities
) {
}
