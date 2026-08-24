package com.memories.platform.guest.dto;

import java.util.List;

public record MemoryGuestPageResponse(
        List<MemoryGuestResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
}
