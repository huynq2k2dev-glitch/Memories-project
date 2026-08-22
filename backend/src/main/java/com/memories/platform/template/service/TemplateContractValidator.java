package com.memories.platform.template.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.memories.platform.template.constants.TemplateConstants;
import com.memories.platform.template.exception.InvalidTemplateContractException;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialects;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class TemplateContractValidator {

    private static final String DRAFT_2020_12_URI = "https://json-schema.org/draft/2020-12/schema";
    private static final Pattern SECTION_TYPE = Pattern.compile("[A-Z][A-Z0-9_]{0,49}");
    private static final int MAXIMUM_SECTION_TYPES = 30;
    private final SchemaRegistry schemaRegistry = SchemaRegistry.withDialect(Dialects.getDraft202012());

    public void validateDraft(
            String componentKey,
            String rendererVersion,
            JsonNode configSchema,
            JsonNode defaultConfig,
            JsonNode sectionContracts,
            List<String> requiredSections
    ) {
        Set<String> rendererVersions = TemplateConstants.ALLOWED_RENDERERS.get(componentKey);
        if (rendererVersions == null || !rendererVersions.contains(rendererVersion)) {
            throw new InvalidTemplateContractException(
                    "TEMPLATE_RENDERER_UNSUPPORTED",
                    "The component key and renderer version are not available in this release."
            );
        }
        if (!configSchema.isObject() || !defaultConfig.isObject()) {
            throw new InvalidTemplateContractException(
                    "TEMPLATE_CONFIG_INVALID",
                    "The config schema and default config must be JSON objects."
            );
        }
        if (new HashSet<>(requiredSections).size() != requiredSections.size()) {
            throw new InvalidTemplateContractException(
                    "TEMPLATE_REQUIRED_SECTIONS_INVALID",
                    "Required section keys must be unique."
            );
        }
        validateSectionContractStructure(sectionContracts, requiredSections);
    }

    public void validateForPublish(JsonNode configSchema, JsonNode defaultConfig) {
        JsonNode declaredDialect = configSchema.get("$schema");
        if (declaredDialect != null
                && (!declaredDialect.isTextual() || !DRAFT_2020_12_URI.equals(declaredDialect.textValue()))) {
            throw schemaInvalid();
        }

        try {
            if (!satisfiesSchema(configSchema, defaultConfig)) {
                throw new InvalidTemplateContractException(
                        "TEMPLATE_DEFAULT_CONFIG_INVALID",
                        "The default config does not satisfy the config schema."
                );
            }
        } catch (InvalidTemplateContractException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw schemaInvalid();
        }
    }

    public void validateSectionContractsForPublish(JsonNode sectionContracts) {
        Iterator<Map.Entry<String, JsonNode>> contracts = sectionContracts.fields();
        while (contracts.hasNext()) {
            JsonNode configSchema = contracts.next().getValue().get("configSchema");
            ensureDialect(configSchema);
            try {
                Schema schema = schemaRegistry.getSchema(
                        configSchema.toString(),
                        InputFormat.JSON
                );
                schema.validate("{}", InputFormat.JSON);
            } catch (RuntimeException exception) {
                throw new InvalidTemplateContractException(
                        "TEMPLATE_SECTION_SCHEMA_INVALID",
                        "A section config schema is not a valid JSON Schema Draft 2020-12 contract."
                );
            }
        }
    }

    public boolean satisfiesSchema(JsonNode configSchema, JsonNode config) {
        Schema schema = schemaRegistry.getSchema(configSchema.toString(), InputFormat.JSON);
        return schema.validate(config.toString(), InputFormat.JSON).isEmpty();
    }

    private void validateSectionContractStructure(
            JsonNode sectionContracts,
            List<String> requiredSections
    ) {
        if (!sectionContracts.isObject() || sectionContracts.size() > MAXIMUM_SECTION_TYPES) {
            throw sectionContractsInvalid();
        }
        Iterator<Map.Entry<String, JsonNode>> contracts = sectionContracts.fields();
        while (contracts.hasNext()) {
            Map.Entry<String, JsonNode> contract = contracts.next();
            JsonNode value = contract.getValue();
            if (!SECTION_TYPE.matcher(contract.getKey()).matches()
                    || !value.isObject()
                    || value.size() != 1
                    || !value.path("configSchema").isObject()) {
                throw sectionContractsInvalid();
            }
        }
        if (!requiredSections.stream().allMatch(sectionContracts::has)) {
            throw sectionContractsInvalid();
        }
    }

    private void ensureDialect(JsonNode configSchema) {
        JsonNode declaredDialect = configSchema.get("$schema");
        if (declaredDialect != null
                && (!declaredDialect.isTextual()
                || !DRAFT_2020_12_URI.equals(declaredDialect.textValue()))) {
            throw new InvalidTemplateContractException(
                    "TEMPLATE_SECTION_SCHEMA_INVALID",
                    "A section config schema must use JSON Schema Draft 2020-12."
            );
        }
    }

    private InvalidTemplateContractException sectionContractsInvalid() {
        return new InvalidTemplateContractException(
                "TEMPLATE_SECTION_CONTRACTS_INVALID",
                "Section contracts must map valid section types to one configSchema object and include every required section."
        );
    }

    private InvalidTemplateContractException schemaInvalid() {
        return new InvalidTemplateContractException(
                "TEMPLATE_SCHEMA_INVALID",
                "The config schema is not a valid JSON Schema Draft 2020-12 contract."
        );
    }
}
