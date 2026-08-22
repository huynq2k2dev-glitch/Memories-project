package com.memories.platform.media.dto;

import java.util.UUID;

public record ReadyMediaAsset(
        UUID id,
        String mimeType,
        long fileSize,
        String deliveryUrl,
        long version
) {
}
