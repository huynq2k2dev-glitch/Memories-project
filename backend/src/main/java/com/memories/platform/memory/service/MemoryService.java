package com.memories.platform.memory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memories.platform.auth.exception.PermissionDeniedException;
import com.memories.platform.auth.service.CurrentActorService;
import com.memories.platform.media.service.MediaAssetAccessService;
import com.memories.platform.memory.dto.CreateMemoryRequest;
import com.memories.platform.memory.dto.MemoryCoverResponse;
import com.memories.platform.memory.dto.MemoryDetailResponse;
import com.memories.platform.memory.dto.UpdateMemoryAssetReferenceRequest;
import com.memories.platform.memory.dto.UpdateMemoryRequest;
import com.memories.platform.memory.constants.MemoryMessageConstants;
import com.memories.platform.memory.entity.Memory;
import com.memories.platform.memory.exception.MemorySlugConflictException;
import com.memories.platform.memory.exception.MemoryTemplateTypeMismatchException;
import com.memories.platform.memory.exception.InvalidMemoryThemeException;
import com.memories.platform.memory.exception.MemoryVersionConflictException;
import com.memories.platform.memory.repository.MemoryRepository;
import com.memories.platform.template.dto.TemplateSelectionResponse;
import com.memories.platform.template.service.TemplateSelectionService;
import com.memories.platform.template.service.TemplateSectionContractService;
import com.memories.platform.template.service.TemplateThemeConfigService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class MemoryService {

    private final MemoryRepository memoryRepository;
    private final TemplateSelectionService templateSelectionService;
    private final CurrentActorService currentActorService;
    private final MemoryAccessService accessService;
    private final MemorySlugService slugService;
    private final MemoryContentSafetyService contentSafetyService;
    private final TemplateThemeConfigService themeConfigService;
    private final TemplateSectionContractService sectionContractService;
    private final MediaAssetAccessService assetAccessService;
    private final MemoryPasswordAccessService passwordAccessService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public MemoryService(
            MemoryRepository memoryRepository,
            TemplateSelectionService templateSelectionService,
            CurrentActorService currentActorService,
            MemoryAccessService accessService,
            MemorySlugService slugService,
            MemoryContentSafetyService contentSafetyService,
            TemplateThemeConfigService themeConfigService,
            TemplateSectionContractService sectionContractService,
            MediaAssetAccessService assetAccessService,
            MemoryPasswordAccessService passwordAccessService,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.memoryRepository = memoryRepository;
        this.templateSelectionService = templateSelectionService;
        this.currentActorService = currentActorService;
        this.accessService = accessService;
        this.slugService = slugService;
        this.contentSafetyService = contentSafetyService;
        this.themeConfigService = themeConfigService;
        this.sectionContractService = sectionContractService;
        this.assetAccessService = assetAccessService;
        this.passwordAccessService = passwordAccessService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public MemoryDetailResponse create(CreateMemoryRequest request) {
        UUID ownerId = currentActorService.userId();
        TemplateSelectionResponse selection = templateSelectionService.selectForNewMemory(
                request.templateVersionId()
        );
        if (selection.memoryType() != request.memoryType()) {
            throw new MemoryTemplateTypeMismatchException();
        }
        contentSafetyService.requireSafeMarkdown(request.title());

        UUID memoryId = UUID.randomUUID();
        Instant now = clock.instant();
        var settings = objectMapper.createObjectNode();
        settings.put(MemoryMessageConstants.MODERATION_SETTING, true);
        Memory memory = new Memory(
                memoryId,
                ownerId,
                selection.templateVersionId(),
                slugService.generate(request.title(), memoryId),
                request.title(),
                request.memoryType(),
                selection.defaultConfig().deepCopy(),
                settings,
                now
        );
        try {
            memoryRepository.saveAndFlush(memory);
        } catch (DataIntegrityViolationException exception) {
            throw new MemorySlugConflictException();
        }
        return toResponse(memory);
    }

    @Transactional(readOnly = true)
    public MemoryDetailResponse get(UUID memoryId) {
        Memory memory = accessService.requireView(memoryId);
        return toResponse(memory);
    }

    @Transactional
    public MemoryDetailResponse update(UUID memoryId, UpdateMemoryRequest request) {
        Memory memory = accessService.requireEditable(memoryId);
        UUID actorId = accessService.actorId();
        if (memory.getVersion() != request.version()) {
            throw new MemoryVersionConflictException(memory.getVersion());
        }
        if (!accessService.capabilities(memory).canChangeAccessPolicy()
                && (memory.getVisibility() != request.visibility()
                || request.accessPassword() != null && !request.accessPassword().isBlank())) {
            throw new PermissionDeniedException();
        }
        contentSafetyService.requireSafeMarkdown(request.title());
        contentSafetyService.requireSafeMarkdown(request.summary());
        if (!request.themeConfig().isObject()
                || !themeConfigService.isValid(memory.getTemplateVersionId(), request.themeConfig())) {
            throw new InvalidMemoryThemeException();
        }

        String accessPasswordHash = passwordAccessService.passwordHashForUpdate(
                memory,
                request.visibility(),
                request.accessPassword()
        );
        boolean passwordChanged = !Objects.equals(
                memory.getAccessPasswordHash(),
                accessPasswordHash
        );

        memory.updateDraft(
                request.title(),
                request.summary(),
                request.visibility(),
                accessPasswordHash,
                request.themeConfig().deepCopy(),
                request.eventStartAt(),
                request.expiresAt(),
                actorId,
                clock.instant()
        );
        try {
            memoryRepository.flush();
            if (passwordChanged) {
                passwordAccessService.revokeAll(memoryId);
            }
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new MemoryVersionConflictException(null);
        }
        return toResponse(memory);
    }

    @Transactional
    public MemoryCoverResponse updateCover(
            UUID memoryId,
            UpdateMemoryAssetReferenceRequest request
    ) {
        Memory memory = accessService.requireEditable(memoryId);
        UUID actorId = accessService.actorId();
        if (memory.getVersion() != request.version()) {
            throw new MemoryVersionConflictException(memory.getVersion());
        }
        if (request.assetId() != null) {
            assetAccessService.requireReadyOwned(request.assetId(), actorId);
        }
        memory.updateCover(request.assetId(), actorId, clock.instant());
        try {
            memoryRepository.flush();
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new MemoryVersionConflictException(null);
        }
        return new MemoryCoverResponse(memory.getCoverAssetId(), memory.getVersion());
    }

    private MemoryDetailResponse toResponse(Memory memory) {
        return new MemoryDetailResponse(
                memory.getId(),
                memory.getOwnerId(),
                memory.getTemplateVersionId(),
                memory.getSlug(),
                memory.getTitle(),
                memory.getMemoryType(),
                memory.getStatus(),
                memory.getVisibility(),
                memory.getSummary(),
                memory.getThemeConfig().deepCopy(),
                memory.getSettings().deepCopy(),
                memory.getCoverAssetId(),
                memory.getEventStartAt(),
                memory.getPublishedAt(),
                memory.getExpiresAt(),
                memory.getCreatedAt(),
                memory.getUpdatedAt(),
                memory.getVersion(),
                sectionContractService.allowedSectionTypes(memory.getTemplateVersionId()),
                accessService.capabilities(memory)
        );
    }
}
