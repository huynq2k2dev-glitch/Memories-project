package com.memories.platform.memory.dto;

import java.time.Instant;

public record MessageModerationSettingsResponse(
        boolean enabled,
        long version,
        Instant updatedAt
) {
}
