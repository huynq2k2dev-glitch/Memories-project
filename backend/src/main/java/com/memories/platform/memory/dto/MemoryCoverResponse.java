package com.memories.platform.memory.dto;

import java.util.UUID;

public record MemoryCoverResponse(
        UUID coverAssetId,
        long version
) {
}
