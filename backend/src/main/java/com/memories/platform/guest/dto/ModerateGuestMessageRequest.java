package com.memories.platform.guest.dto;

import com.memories.platform.guest.entity.GuestMessageStatus;
import jakarta.validation.constraints.NotNull;

public record ModerateGuestMessageRequest(
        @NotNull GuestMessageStatus status
) {
}
