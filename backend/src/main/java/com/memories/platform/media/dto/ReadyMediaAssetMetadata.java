package com.memories.platform.media.dto;

import java.util.UUID;

public record ReadyMediaAssetMetadata(
        UUID id,
        String mimeType,
        long fileSize
) {
}
