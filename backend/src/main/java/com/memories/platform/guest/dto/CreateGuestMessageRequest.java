package com.memories.platform.guest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGuestMessageRequest(
        @NotBlank @Size(max = 200) String guestName,
        @NotBlank @Size(max = 2000) String content
) {
}
