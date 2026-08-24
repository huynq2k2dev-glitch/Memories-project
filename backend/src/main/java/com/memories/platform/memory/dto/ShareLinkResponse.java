package com.memories.platform.memory.dto;

import com.memories.platform.memory.entity.ShareLinkPermission;
import com.memories.platform.memory.entity.ShareLinkStatus;

import java.time.Instant;
import java.util.UUID;

public record ShareLinkResponse(
        UUID id,
        ShareLinkPermission permission,
        UUID guestId,
        Instant expiresAt,
        Integer maxUses,
        int useCount,
        ShareLinkStatus status,
        UUID createdBy,
        Instant createdAt,
        Instant revokedAt
) {
}
