package com.memories.platform.template.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.memories.platform.template.dto.TemplateSectionContractCheckResponse;
import com.memories.platform.template.entity.TemplateVersion;
import com.memories.platform.template.exception.TemplateVersionNotFoundException;
import com.memories.platform.template.repository.TemplateVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TemplateSectionContractService {

    private final TemplateVersionRepository versionRepository;
    private final TemplateContractValidator contractValidator;

    public TemplateSectionContractService(
            TemplateVersionRepository versionRepository,
            TemplateContractValidator contractValidator
    ) {
        this.versionRepository = versionRepository;
        this.contractValidator = contractValidator;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public TemplateSectionContractCheckResponse check(
            UUID templateVersionId,
            String sectionType,
            JsonNode config
    ) {
        TemplateVersion version = versionRepository.findById(templateVersionId)
                .orElseThrow(TemplateVersionNotFoundException::new);
        JsonNode contract = version.getSectionContracts().get(sectionType);
        if (contract == null) {
            return new TemplateSectionContractCheckResponse(false, false, false);
        }
        boolean configValid;
        try {
            configValid = config.isObject()
                    && contractValidator.satisfiesSchema(contract.get("configSchema"), config);
        } catch (RuntimeException exception) {
            configValid = false;
        }
        return new TemplateSectionContractCheckResponse(
                true,
                contains(version.getRequiredSections(), sectionType),
                configValid
        );
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public Set<String> requiredSectionTypes(UUID templateVersionId) {
        TemplateVersion version = versionRepository.findById(templateVersionId)
                .orElseThrow(TemplateVersionNotFoundException::new);
        Set<String> requiredTypes = new HashSet<>();
        version.getRequiredSections().forEach(value -> requiredTypes.add(value.textValue()));
        return Set.copyOf(requiredTypes);
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public List<String> allowedSectionTypes(UUID templateVersionId) {
        TemplateVersion version = versionRepository.findById(templateVersionId)
                .orElseThrow(TemplateVersionNotFoundException::new);
        List<String> types = new ArrayList<>();
        version.getSectionContracts().fieldNames().forEachRemaining(types::add);
        return List.copyOf(types);
    }

    private boolean contains(JsonNode values, String expected) {
        for (JsonNode value : values) {
            if (expected.equals(value.textValue())) {
                return true;
            }
        }
        return false;
    }
}
