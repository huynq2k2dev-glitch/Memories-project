package com.memories.platform.guest.dto;

import com.memories.platform.guest.entity.MemoryGuestStatus;

import java.time.Instant;
import java.util.UUID;

public record MemoryGuestResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        String guestGroup,
        int maxPartySize,
        String note,
        MemoryGuestStatus status,
        boolean tokenIssued,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
}
