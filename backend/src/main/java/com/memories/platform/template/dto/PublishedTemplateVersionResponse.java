package com.memories.platform.template.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

public record PublishedTemplateVersionResponse(
        UUID id,
        int versionNo,
        String componentKey,
        String rendererVersion,
        boolean coverRequired,
        JsonNode defaultConfig,
        List<String> allowedSectionTypes,
        List<String> requiredSections,
        HtmlBook book
) {
}
