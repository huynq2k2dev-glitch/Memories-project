package com.memories.platform.auth.dto;

import com.memories.platform.auth.entity.UserStatus;

import java.util.UUID;

public record CurrentAccountResponse(
        UUID id,
        String email,
        String displayName,
        UserStatus status
) {
}
