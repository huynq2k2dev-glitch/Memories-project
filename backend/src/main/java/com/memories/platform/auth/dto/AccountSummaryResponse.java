package com.memories.platform.auth.dto;

import java.util.UUID;

public record AccountSummaryResponse(
        UUID id,
        String displayName,
        boolean active
) {
}
