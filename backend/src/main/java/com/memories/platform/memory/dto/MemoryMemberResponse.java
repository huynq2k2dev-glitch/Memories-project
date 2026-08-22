package com.memories.platform.memory.dto;

import java.time.Instant;
import java.util.UUID;

public record MemoryMemberResponse(
        UUID id,
        String roleCode,
        String fullName,
        String displayName,
        String description,
        UUID avatarAssetId,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
}
