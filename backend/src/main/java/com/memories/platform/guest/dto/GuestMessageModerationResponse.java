package com.memories.platform.guest.dto;

import com.memories.platform.guest.entity.GuestMessageStatus;

import java.time.Instant;
import java.util.UUID;

public record GuestMessageModerationResponse(
        UUID id,
        String guestName,
        String content,
        GuestMessageStatus status,
        UUID moderatedBy,
        Instant moderatedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
