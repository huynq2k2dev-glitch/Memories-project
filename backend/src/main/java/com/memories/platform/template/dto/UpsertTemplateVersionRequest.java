package com.memories.platform.template.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpsertTemplateVersionRequest(
        @NotBlank @Size(max = 150) String componentKey,
        @NotBlank @Size(max = 50) String rendererVersion,
        boolean coverRequired,
        @NotNull JsonNode configSchema,
        @NotNull JsonNode defaultConfig,
        @NotNull JsonNode sectionContracts,
        @NotNull
        @Size(max = 30)
        List<
                @Valid
                @NotBlank
                @Size(max = 50)
                @Pattern(regexp = "[A-Z][A-Z0-9_]*")
                String
        > requiredSections
) {
    public UpsertTemplateVersionRequest {
        componentKey = componentKey == null ? null : componentKey.trim();
        rendererVersion = rendererVersion == null ? null : rendererVersion.trim();
        requiredSections = requiredSections == null ? null : List.copyOf(requiredSections);
    }
}
