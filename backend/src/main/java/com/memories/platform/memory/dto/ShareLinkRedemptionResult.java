package com.memories.platform.memory.dto;

import com.memories.platform.memory.entity.ShareLinkPermission;

import java.time.Instant;
import java.util.UUID;

public record ShareLinkRedemptionResult(
        UUID memoryId,
        String slug,
        ShareLinkPermission permission,
        Instant expiresAt
) {
}
