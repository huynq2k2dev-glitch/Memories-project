package com.memories.platform.memory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateMessageModerationRequest(
        @NotNull Boolean enabled,
        @NotNull @PositiveOrZero Long version
) {
}
