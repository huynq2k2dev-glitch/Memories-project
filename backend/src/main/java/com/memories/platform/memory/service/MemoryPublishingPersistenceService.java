package com.memories.platform.memory.service;

import com.memories.platform.audit.dto.AuditResult;
import com.memories.platform.audit.service.AuditLogService;
import com.memories.platform.media.service.MediaAssetAccessService;
import com.memories.platform.memory.constants.MemoryPublishingConstants;
import com.memories.platform.memory.dto.PublishMemoryRequest;
import com.memories.platform.memory.dto.PublishMemoryResponse;
import com.memories.platform.memory.entity.Memory;
import com.memories.platform.memory.entity.MemoryImage;
import com.memories.platform.memory.entity.MemoryMember;
import com.memories.platform.memory.entity.MemorySection;
import com.memories.platform.memory.exception.InvalidMemorySectionContractException;
import com.memories.platform.memory.exception.InvalidMemoryThemeException;
import com.memories.platform.memory.exception.MemoryNotEditableException;
import com.memories.platform.memory.exception.MemoryPublishValidationException;
import com.memories.platform.memory.exception.MemoryVersionConflictException;
import com.memories.platform.memory.repository.MemoryImageRepository;
import com.memories.platform.memory.repository.MemoryMemberRepository;
import com.memories.platform.memory.repository.MemoryRepository;
import com.memories.platform.memory.repository.MemorySectionRepository;
import com.memories.platform.template.dto.TemplateRenderContractResponse;
import com.memories.platform.template.dto.TemplateSectionContractCheckResponse;
import com.memories.platform.template.service.TemplateRenderContractService;
import com.memories.platform.template.service.TemplateSectionContractService;
import com.memories.platform.template.service.TemplateThemeConfigService;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class MemoryPublishingPersistenceService {

    private final MemoryRepository memoryRepository;
    private final MemoryAccessService accessService;
    private final MemoryMemberRepository memberRepository;
    private final MemorySectionRepository sectionRepository;
    private final MemoryImageRepository imageRepository;
    private final TemplateRenderContractService renderContractService;
    private final TemplateThemeConfigService themeConfigService;
    private final TemplateSectionContractService sectionContractService;
    private final MediaAssetAccessService assetAccessService;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public MemoryPublishingPersistenceService(
            MemoryRepository memoryRepository,
            MemoryAccessService accessService,
            MemoryMemberRepository memberRepository,
            MemorySectionRepository sectionRepository,
            MemoryImageRepository imageRepository,
            TemplateRenderContractService renderContractService,
            TemplateThemeConfigService themeConfigService,
            TemplateSectionContractService sectionContractService,
            MediaAssetAccessService assetAccessService,
            AuditLogService auditLogService,
            Clock clock
    ) {
        this.memoryRepository = memoryRepository;
        this.accessService = accessService;
        this.memberRepository = memberRepository;
        this.sectionRepository = sectionRepository;
        this.imageRepository = imageRepository;
        this.renderContractService = renderContractService;
        this.themeConfigService = themeConfigService;
        this.sectionContractService = sectionContractService;
        this.assetAccessService = assetAccessService;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    @Transactional
    public PublishMemoryResponse publish(
            UUID memoryId,
            UUID actorId,
            PublishMemoryRequest request,
            String correlationId
    ) {
        Memory memory = accessService.requirePublish(memoryId);
        if (!memory.isDraft()) {
            throw new MemoryNotEditableException();
        }
        if (request.version() == null || memory.getVersion() != request.version()) {
            throw new MemoryVersionConflictException(memory.getVersion());
        }
        if (!MemoryPublishingConstants.PUBLISHABLE_VISIBILITIES.contains(memory.getVisibility())) {
            throw validation(
                    "MEMORY_PUBLISH_VISIBILITY_INVALID",
                    "The memory visibility is not publishable."
            );
        }

        Instant now = clock.instant();
        if (memory.getExpiresAt() != null && !memory.getExpiresAt().isAfter(now)) {
            throw validation(
                    "MEMORY_PUBLISH_EXPIRED",
                    "The memory expiration must be later than the publish time."
            );
        }

        TemplateRenderContractResponse contract = renderContractService.requirePublishable(
                memory.getTemplateVersionId()
        );
        if (!memory.getThemeConfig().isObject()
                || !themeConfigService.isValid(
                        memory.getTemplateVersionId(),
                        memory.getThemeConfig()
                )) {
            throw new InvalidMemoryThemeException();
        }

        List<MemorySection> sections = sectionRepository.findAllByMemoryIdOrderBySortOrderAsc(
                memoryId
        );
        validateSections(memory, contract, sections);
        validateAssets(memory, memberRepository.findAllByMemoryIdOrderBySortOrderAsc(memoryId));

        memory.publish(actorId, now);
        try {
            memoryRepository.flush();
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new MemoryVersionConflictException(null);
        }
        auditLogService.record(
                actorId,
                MemoryPublishingConstants.AUDIT_ACTION,
                MemoryPublishingConstants.AUDIT_ENTITY_TYPE,
                memoryId,
                AuditResult.SUCCESS,
                correlationId,
                null
        );
        return new PublishMemoryResponse(
                memory.getId(),
                memory.getSlug(),
                memory.getStatus(),
                memory.getVisibility(),
                memory.getPublishedAt(),
                memory.getVersion()
        );
    }

    private void validateSections(
            Memory memory,
            TemplateRenderContractResponse contract,
            List<MemorySection> sections
    ) {
        for (MemorySection section : sections) {
            TemplateSectionContractCheckResponse check = sectionContractService.check(
                    memory.getTemplateVersionId(),
                    section.getSectionType(),
                    section.getConfig()
            );
            if (!check.allowed() || !check.configValid()) {
                throw new InvalidMemorySectionContractException();
            }
        }

        Set<String> completeTypes = new HashSet<>();
        sections.stream()
                .filter(MemorySection::hasContent)
                .map(MemorySection::getSectionType)
                .forEach(completeTypes::add);
        if (!completeTypes.containsAll(contract.requiredSectionTypes())) {
            throw validation(
                    "MEMORY_REQUIRED_SECTIONS_INCOMPLETE",
                    "Every required section type must have visible content before publishing."
            );
        }
        if (contract.coverRequired() && memory.getCoverAssetId() == null) {
            throw validation(
                    "MEMORY_COVER_REQUIRED",
                    "The selected template requires a cover image before publishing."
            );
        }
    }

    private void validateAssets(Memory memory, List<MemoryMember> members) {
        Set<UUID> assetIds = new HashSet<>();
        if (memory.getCoverAssetId() != null) {
            assetIds.add(memory.getCoverAssetId());
        }
        members.stream()
                .map(MemoryMember::getAvatarAssetId)
                .filter(Objects::nonNull)
                .forEach(assetIds::add);
        imageRepository.findAllByMemoryIdOrderBySortOrderAsc(memory.getId()).stream()
                .map(MemoryImage::getMediaAssetId)
                .forEach(assetIds::add);
        assetAccessService.readyMetadata(assetIds);
    }

    private MemoryPublishValidationException validation(String code, String detail) {
        return new MemoryPublishValidationException(code, detail);
    }
}
