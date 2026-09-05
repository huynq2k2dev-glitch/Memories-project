package com.memories.platform.template.dto;

import java.util.Set;
import java.util.UUID;

public record TemplateRenderContractResponse(
        UUID templateVersionId,
        String componentKey,
        String rendererVersion,
        boolean coverRequired,
        Set<String> requiredSectionTypes,
        HtmlBook book
) {
}
