package com.memories.platform.memory.dto;

import com.memories.platform.memory.entity.ShareLinkPermission;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.UUID;

public record CreateShareLinkRequest(
        @NotNull ShareLinkPermission permission,
        UUID guestId,
        Instant expiresAt,
        @Positive Integer maxUses
) {
}
