package com.memories.platform.template.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.memories.platform.template.entity.TemplateVersionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminTemplateVersionResponse(
        UUID id,
        int versionNo,
        String componentKey,
        String rendererVersion,
        boolean coverRequired,
        JsonNode configSchema,
        JsonNode defaultConfig,
        JsonNode sectionContracts,
        List<String> requiredSections,
        TemplateVersionStatus status,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        HtmlBook book
) {
}
