package com.memories.platform.media.dto;

import com.memories.platform.media.entity.MediaAssetStatus;

import java.time.Instant;
import java.util.UUID;

public record MediaAssetResponse(
        UUID id,
        String originalFileName,
        String mimeType,
        long fileSize,
        MediaAssetStatus status,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
}
