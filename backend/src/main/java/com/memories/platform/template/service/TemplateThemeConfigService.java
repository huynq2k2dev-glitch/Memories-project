package com.memories.platform.template.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.memories.platform.template.exception.TemplateVersionNotFoundException;
import com.memories.platform.template.repository.TemplateVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TemplateThemeConfigService {

    private final TemplateVersionRepository versionRepository;
    private final TemplateContractValidator contractValidator;

    public TemplateThemeConfigService(
            TemplateVersionRepository versionRepository,
            TemplateContractValidator contractValidator
    ) {
        this.versionRepository = versionRepository;
        this.contractValidator = contractValidator;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean isValid(UUID templateVersionId, JsonNode themeConfig) {
        return versionRepository.findById(templateVersionId)
                .map(version -> contractValidator.satisfiesSchema(
                        version.getConfigSchema(),
                        themeConfig
                ))
                .orElseThrow(TemplateVersionNotFoundException::new);
    }
}
