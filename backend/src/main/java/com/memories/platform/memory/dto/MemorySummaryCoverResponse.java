package com.memories.platform.memory.dto;

import java.util.UUID;

public record MemorySummaryCoverResponse(
        UUID id,
        String mimeType,
        String deliveryUrl
) {
}
