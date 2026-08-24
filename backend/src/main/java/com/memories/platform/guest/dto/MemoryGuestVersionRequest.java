package com.memories.platform.guest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record MemoryGuestVersionRequest(
        @NotNull @PositiveOrZero Long version
) {
}
