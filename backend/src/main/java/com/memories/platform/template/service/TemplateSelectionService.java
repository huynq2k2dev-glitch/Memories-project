package com.memories.platform.template.service;

import com.memories.platform.template.dto.TemplateSelectionResponse;
import com.memories.platform.template.entity.TemplateStatus;
import com.memories.platform.template.entity.TemplateVersion;
import com.memories.platform.template.entity.TemplateVersionStatus;
import com.memories.platform.template.exception.TemplateVersionNotSelectableException;
import com.memories.platform.template.repository.TemplateVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TemplateSelectionService {

    private final TemplateVersionRepository versionRepository;

    public TemplateSelectionService(TemplateVersionRepository versionRepository) {
        this.versionRepository = versionRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public TemplateSelectionResponse selectForNewMemory(UUID templateVersionId) {
        TemplateVersion version = versionRepository.findForSelection(templateVersionId)
                .filter(candidate -> candidate.getStatus() == TemplateVersionStatus.PUBLISHED)
                .filter(candidate -> candidate.getTemplate().getStatus() == TemplateStatus.ACTIVE)
                .orElseThrow(TemplateVersionNotSelectableException::new);
        return new TemplateSelectionResponse(
                version.getId(),
                version.getTemplate().getMemoryType(),
                version.getDefaultConfig().deepCopy()
        );
    }
}
