package com.memories.platform.template.service;

import com.memories.platform.template.dto.TemplateRenderContractResponse;
import com.memories.platform.template.entity.TemplateStatus;
import com.memories.platform.template.entity.TemplateVersion;
import com.memories.platform.template.entity.TemplateVersionStatus;
import com.memories.platform.template.exception.TemplateVersionNotFoundException;
import com.memories.platform.template.exception.TemplateVersionNotSelectableException;
import com.memories.platform.template.repository.TemplateVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class TemplateRenderContractService {

    private final TemplateVersionRepository versionRepository;

    public TemplateRenderContractService(TemplateVersionRepository versionRepository) {
        this.versionRepository = versionRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public TemplateRenderContractResponse requireRenderable(UUID templateVersionId) {
        return toResponse(find(templateVersionId));
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public TemplateRenderContractResponse requirePublishable(UUID templateVersionId) {
        TemplateVersion version = find(templateVersionId);
        if (version.getStatus() != TemplateVersionStatus.PUBLISHED
                || version.getTemplate().getStatus() != TemplateStatus.ACTIVE) {
            throw new TemplateVersionNotSelectableException();
        }
        return toResponse(version);
    }

    private TemplateVersion find(UUID templateVersionId) {
        return versionRepository.findForSelection(templateVersionId)
                .orElseThrow(TemplateVersionNotFoundException::new);
    }

    private TemplateRenderContractResponse toResponse(TemplateVersion version) {
        HtmlBookValidator.validate(version.getComponentKey(), version.getBook());
        Set<String> requiredSectionTypes = new HashSet<>();
        version.getRequiredSections().forEach(value -> requiredSectionTypes.add(value.textValue()));
        return new TemplateRenderContractResponse(
                version.getId(),
                version.getComponentKey(),
                version.getRendererVersion(),
                version.isCoverRequired(),
                Set.copyOf(requiredSectionTypes),
                version.getBook()
        );
    }
}
