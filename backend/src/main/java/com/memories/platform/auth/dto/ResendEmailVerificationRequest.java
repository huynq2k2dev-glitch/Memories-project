package com.memories.platform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResendEmailVerificationRequest(
        @NotBlank @Email @Size(max = 320) String email
) {
    public ResendEmailVerificationRequest {
        email = email == null ? null : email.trim();
    }
}
