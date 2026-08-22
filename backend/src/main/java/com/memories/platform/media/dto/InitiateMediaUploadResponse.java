package com.memories.platform.media.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record InitiateMediaUploadResponse(
        UUID assetId,
        String uploadUrl,
        String method,
        Map<String, String> requiredHeaders,
        Instant expiresAt
) {
}
