package com.memories.platform.template.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memories.platform.template.dto.AdminTemplateResponse;
import com.memories.platform.template.dto.AdminTemplatePageResponse;
import com.memories.platform.template.dto.AdminTemplateVersionResponse;
import com.memories.platform.template.dto.CreateTemplateRequest;
import com.memories.platform.template.dto.UpdateTemplateRequest;
import com.memories.platform.template.dto.UpsertTemplateVersionRequest;
import com.memories.platform.template.entity.Template;
import com.memories.platform.template.entity.TemplateVersion;
import com.memories.platform.template.entity.TemplateVersionStatus;
import com.memories.platform.template.exception.TemplateCodeAlreadyExistsException;
import com.memories.platform.template.exception.InvalidAdminTemplateQueryException;
import com.memories.platform.template.exception.TemplateNotFoundException;
import com.memories.platform.template.exception.TemplateVersionImmutableException;
import com.memories.platform.template.exception.TemplateVersionNotFoundException;
import com.memories.platform.template.repository.TemplateRepository;
import com.memories.platform.template.repository.TemplateVersionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.memories.platform.template.constants.TemplateConstants.MAXIMUM_PAGE_SIZE;

@Service
public class TemplateAdministrationPersistenceService {

    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository versionRepository;
    private final TemplateContractValidator contractValidator;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public TemplateAdministrationPersistenceService(
            TemplateRepository templateRepository,
            TemplateVersionRepository versionRepository,
            TemplateContractValidator contractValidator,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.templateRepository = templateRepository;
        this.versionRepository = versionRepository;
        this.contractValidator = contractValidator;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminTemplatePageResponse list(int pageNumber, int pageSize) {
        if (pageNumber < 0 || pageSize < 1 || pageSize > MAXIMUM_PAGE_SIZE) {
            throw new InvalidAdminTemplateQueryException();
        }
        Page<Template> templatePage = templateRepository.findAll(PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        ));
        List<UUID> templateIds = templatePage.getContent().stream()
                .map(Template::getId)
                .toList();
        Map<UUID, List<TemplateVersion>> versionsByTemplate = templateIds.isEmpty()
                ? Map.of()
                : versionRepository.findForAdministration(templateIds).stream()
                        .collect(Collectors.groupingBy(version -> version.getTemplate().getId()));
        List<AdminTemplateResponse> items = templatePage.getContent().stream()
                .map(template -> toResponse(
                        template,
                        versionsByTemplate.getOrDefault(template.getId(), List.of())
                ))
                .toList();
        return new AdminTemplatePageResponse(
                items,
                templatePage.getNumber(),
                templatePage.getSize(),
                templatePage.getTotalElements(),
                templatePage.getTotalPages()
        );
    }

    @Transactional
    public AdminTemplateResponse create(CreateTemplateRequest request) {
        if (templateRepository.existsByCode(request.code())) {
            throw new TemplateCodeAlreadyExistsException();
        }

        Instant now = clock.instant();
        Template template = new Template(
                UUID.randomUUID(),
                request.code(),
                request.name(),
                request.memoryType(),
                request.description(),
                now
        );
        try {
            templateRepository.saveAndFlush(template);
        } catch (DataIntegrityViolationException exception) {
            throw new TemplateCodeAlreadyExistsException();
        }
        return toResponse(template);
    }

    @Transactional
    public AdminTemplateResponse update(UUID templateId, UpdateTemplateRequest request) {
        Template template = templateRepository.findForUpdateById(templateId)
                .orElseThrow(TemplateNotFoundException::new);
        template.updateMetadata(
                request.name(),
                request.memoryType(),
                request.description(),
                request.status(),
                clock.instant()
        );
        return toResponse(template);
    }

    @Transactional
    public AdminTemplateVersionResponse createVersion(
            UUID templateId,
            UpsertTemplateVersionRequest request
    ) {
        contractValidator.validateDraft(
                request.componentKey(),
                request.rendererVersion(),
                request.configSchema(),
                request.defaultConfig(),
                request.sectionContracts(),
                request.requiredSections()
        );
        Template template = templateRepository.findForUpdateById(templateId)
                .orElseThrow(TemplateNotFoundException::new);
        int nextVersionNumber = versionRepository.maximumVersionNumber(templateId) + 1;
        Instant now = clock.instant();
        TemplateVersion version = new TemplateVersion(
                UUID.randomUUID(),
                template,
                nextVersionNumber,
                request.componentKey(),
                request.rendererVersion(),
                request.coverRequired(),
                request.configSchema().deepCopy(),
                request.defaultConfig().deepCopy(),
                objectMapper.valueToTree(request.requiredSections()),
                request.sectionContracts().deepCopy(),
                now
        );
        versionRepository.save(version);
        return toResponse(version);
    }

    @Transactional
    public AdminTemplateVersionResponse updateVersion(
            UUID templateId,
            UUID versionId,
            UpsertTemplateVersionRequest request
    ) {
        contractValidator.validateDraft(
                request.componentKey(),
                request.rendererVersion(),
                request.configSchema(),
                request.defaultConfig(),
                request.sectionContracts(),
                request.requiredSections()
        );
        TemplateVersion version = findVersionForUpdate(templateId, versionId);
        if (!version.isDraft()) {
            throw new TemplateVersionImmutableException();
        }
        version.updateDraft(
                request.componentKey(),
                request.rendererVersion(),
                request.coverRequired(),
                request.configSchema().deepCopy(),
                request.defaultConfig().deepCopy(),
                objectMapper.valueToTree(request.requiredSections()),
                request.sectionContracts().deepCopy(),
                clock.instant()
        );
        return toResponse(version);
    }

    @Transactional
    public AdminTemplateVersionResponse publish(UUID templateId, UUID versionId) {
        TemplateVersion version = findVersionForUpdate(templateId, versionId);
        if (version.isPublished()) {
            return toResponse(version);
        }
        if (!version.isDraft()) {
            throw new TemplateVersionImmutableException();
        }
        contractValidator.validateDraft(
                version.getComponentKey(),
                version.getRendererVersion(),
                version.getConfigSchema(),
                version.getDefaultConfig(),
                version.getSectionContracts(),
                requiredSections(version)
        );
        contractValidator.validateForPublish(version.getConfigSchema(), version.getDefaultConfig());
        contractValidator.validateSectionContractsForPublish(version.getSectionContracts());
        version.publish(clock.instant());
        return toResponse(version);
    }

    @Transactional
    public AdminTemplateVersionResponse deprecate(UUID templateId, UUID versionId) {
        TemplateVersion version = findVersionForUpdate(templateId, versionId);
        if (version.getStatus() == TemplateVersionStatus.DEPRECATED) {
            return toResponse(version);
        }
        if (!version.isPublished()) {
            throw new TemplateVersionImmutableException();
        }
        version.deprecate(clock.instant());
        return toResponse(version);
    }

    private TemplateVersion findVersionForUpdate(UUID templateId, UUID versionId) {
        return versionRepository.findForUpdate(templateId, versionId)
                .orElseThrow(TemplateVersionNotFoundException::new);
    }

    private AdminTemplateResponse toResponse(Template template) {
        return toResponse(template, template.getVersions());
    }

    private AdminTemplateResponse toResponse(
            Template template,
            List<TemplateVersion> versions
    ) {
        return new AdminTemplateResponse(
                template.getId(),
                template.getCode(),
                template.getName(),
                template.getMemoryType(),
                template.getDescription(),
                template.getStatus(),
                template.getVersion(),
                template.getCreatedAt(),
                template.getUpdatedAt(),
                versions.stream().map(this::toResponse).toList()
        );
    }

    private AdminTemplateVersionResponse toResponse(TemplateVersion version) {
        return new AdminTemplateVersionResponse(
                version.getId(),
                version.getVersionNo(),
                version.getComponentKey(),
                version.getRendererVersion(),
                version.isCoverRequired(),
                version.getConfigSchema().deepCopy(),
                version.getDefaultConfig().deepCopy(),
                version.getSectionContracts().deepCopy(),
                requiredSections(version),
                version.getStatus(),
                version.getPublishedAt(),
                version.getCreatedAt(),
                version.getUpdatedAt()
        );
    }

    private List<String> requiredSections(TemplateVersion version) {
        List<String> sections = new ArrayList<>();
        version.getRequiredSections().forEach(section -> sections.add(section.textValue()));
        return List.copyOf(sections);
    }
}
