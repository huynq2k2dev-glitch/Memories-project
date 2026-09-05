package com.memories.platform.template.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.memories.platform.common.domain.MemoryType;
import com.memories.platform.template.dto.PublishedTemplateVersionResponse;
import com.memories.platform.template.dto.TemplateCatalogItemResponse;
import com.memories.platform.template.dto.TemplateCatalogPageResponse;
import com.memories.platform.template.entity.Template;
import com.memories.platform.template.entity.TemplateStatus;
import com.memories.platform.template.entity.TemplateVersion;
import com.memories.platform.template.entity.TemplateVersionStatus;
import com.memories.platform.template.exception.InvalidTemplateCatalogQueryException;
import com.memories.platform.template.repository.TemplateRepository;
import com.memories.platform.template.repository.TemplateVersionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TemplateCatalogService {

    private static final int MAXIMUM_PAGE_SIZE = 50;

    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository versionRepository;

    public TemplateCatalogService(
            TemplateRepository templateRepository,
            TemplateVersionRepository versionRepository
    ) {
        this.templateRepository = templateRepository;
        this.versionRepository = versionRepository;
    }

    @Transactional(readOnly = true)
    public TemplateCatalogPageResponse list(
            int pageNumber,
            int pageSize,
            MemoryType memoryType,
            TemplateStatus requestedStatus
    ) {
        if (pageNumber < 0 || pageSize < 1 || pageSize > MAXIMUM_PAGE_SIZE) {
            throw new InvalidTemplateCatalogQueryException();
        }
        if (requestedStatus != null && requestedStatus != TemplateStatus.ACTIVE) {
            return new TemplateCatalogPageResponse(List.of(), pageNumber, pageSize, 0, 0);
        }

        PageRequest pageRequest = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<Template> templatePage = templateRepository.findCatalog(
                TemplateStatus.ACTIVE,
                memoryType,
                TemplateVersionStatus.PUBLISHED,
                pageRequest
        );
        List<UUID> templateIds = templatePage.getContent().stream().map(Template::getId).toList();
        Map<UUID, List<TemplateVersion>> versionsByTemplate = publishedVersionsByTemplate(templateIds);
        List<TemplateCatalogItemResponse> items = templatePage.getContent().stream()
                .map(template -> toResponse(
                        template,
                        versionsByTemplate.getOrDefault(template.getId(), List.of())
                ))
                .toList();

        return new TemplateCatalogPageResponse(
                items,
                templatePage.getNumber(),
                templatePage.getSize(),
                templatePage.getTotalElements(),
                templatePage.getTotalPages()
        );
    }

    private Map<UUID, List<TemplateVersion>> publishedVersionsByTemplate(List<UUID> templateIds) {
        if (templateIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return versionRepository.findForCatalog(templateIds, TemplateVersionStatus.PUBLISHED).stream()
                .collect(Collectors.groupingBy(version -> version.getTemplate().getId()));
    }

    private TemplateCatalogItemResponse toResponse(
            Template template,
            List<TemplateVersion> versions
    ) {
        return new TemplateCatalogItemResponse(
                template.getId(),
                template.getCode(),
                template.getName(),
                template.getMemoryType(),
                template.getDescription(),
                versions.stream().map(this::toResponse).toList()
        );
    }

    private PublishedTemplateVersionResponse toResponse(TemplateVersion version) {
        return new PublishedTemplateVersionResponse(
                version.getId(),
                version.getVersionNo(),
                version.getComponentKey(),
                version.getRendererVersion(),
                version.isCoverRequired(),
                version.getDefaultConfig().deepCopy(),
                allowedSectionTypes(version.getSectionContracts()),
                requiredSections(version.getRequiredSections()),
                version.getBook()
        );
    }

    private List<String> allowedSectionTypes(JsonNode sectionContracts) {
        List<String> types = new ArrayList<>();
        sectionContracts.fieldNames().forEachRemaining(types::add);
        return List.copyOf(types);
    }

    private List<String> requiredSections(JsonNode requiredSections) {
        List<String> sections = new ArrayList<>();
        requiredSections.forEach(section -> sections.add(section.textValue()));
        return List.copyOf(sections);
    }
}
