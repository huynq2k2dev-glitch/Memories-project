package com.memories.platform.memory.dto;

import java.util.List;

public record MemoryPageResponse(
        List<MemorySummaryResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
