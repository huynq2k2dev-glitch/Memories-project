package com.memories.platform.template.dto;

import com.memories.platform.common.domain.MemoryType;

import java.util.List;
import java.util.UUID;

public record TemplateCatalogItemResponse(
        UUID id,
        String code,
        String name,
        MemoryType memoryType,
        String description,
        List<PublishedTemplateVersionResponse> versions
) {
}
