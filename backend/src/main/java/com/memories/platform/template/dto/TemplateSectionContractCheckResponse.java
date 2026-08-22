package com.memories.platform.template.dto;

public record TemplateSectionContractCheckResponse(
        boolean allowed,
        boolean required,
        boolean configValid
) {
}
