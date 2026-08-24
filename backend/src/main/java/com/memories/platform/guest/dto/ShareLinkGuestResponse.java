package com.memories.platform.guest.dto;

import java.util.UUID;

public record ShareLinkGuestResponse(
        UUID id,
        String fullName,
        String guestGroup,
        int maxPartySize
) {
}
