package com.memories.platform.template.dto;

import java.util.List;

public record TemplateCatalogPageResponse(
        List<TemplateCatalogItemResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
}
