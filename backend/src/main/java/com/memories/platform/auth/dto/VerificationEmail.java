package com.memories.platform.auth.dto;

import java.net.URI;
import java.time.Instant;

public record VerificationEmail(
        String recipient,
        URI verificationUri,
        Instant expiresAt
) {
}
