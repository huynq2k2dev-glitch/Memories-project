package com.memories.platform.memory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record PublishMemoryRequest(
        @NotNull @PositiveOrZero Long version
) {
}
