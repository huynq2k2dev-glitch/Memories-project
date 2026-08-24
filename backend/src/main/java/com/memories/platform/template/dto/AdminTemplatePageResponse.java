package com.memories.platform.template.dto;

import java.util.List;

public record AdminTemplatePageResponse(
        List<AdminTemplateResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
}
