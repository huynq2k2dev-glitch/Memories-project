package com.memories.platform.guest.dto;

import java.time.Instant;
import java.util.UUID;

public record GuestAccessTokenResponse(
        UUID guestId,
        String accessToken,
        String invitationPath,
        Instant issuedAt,
        long version
) {
}
