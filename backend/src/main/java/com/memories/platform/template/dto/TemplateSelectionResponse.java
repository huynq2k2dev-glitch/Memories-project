package com.memories.platform.template.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.memories.platform.common.domain.MemoryType;

import java.util.UUID;

public record TemplateSelectionResponse(
        UUID templateVersionId,
        MemoryType memoryType,
        JsonNode defaultConfig
) {
}
