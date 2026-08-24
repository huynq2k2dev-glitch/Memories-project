package com.memories.platform.guest.dto;

import java.time.Instant;
import java.util.UUID;

public record GuestMessagePublicResponse(
        UUID id,
        String guestName,
        String content,
        Instant createdAt
) {
}
