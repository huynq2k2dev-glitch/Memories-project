package com.memories.platform.memory.service;

import com.memories.platform.memory.dto.CreateMemorySectionRequest;
import com.memories.platform.memory.dto.MemorySectionResponse;
import com.memories.platform.memory.dto.ReorderMemoryItemsRequest;
import com.memories.platform.memory.dto.UpdateMemorySectionRequest;
import com.memories.platform.memory.entity.Memory;
import com.memories.platform.memory.entity.MemorySection;
import com.memories.platform.memory.exception.InvalidMemoryItemOrderException;
import com.memories.platform.memory.exception.InvalidMemorySectionContractException;
import com.memories.platform.memory.exception.MemorySectionConflictException;
import com.memories.platform.memory.exception.MemorySectionNotFoundException;
import com.memories.platform.memory.exception.MemoryVersionConflictException;
import com.memories.platform.memory.repository.MemorySectionRepository;
import com.memories.platform.template.dto.TemplateSectionContractCheckResponse;
import com.memories.platform.template.service.TemplateSectionContractService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class MemorySectionService {

    private final MemorySectionRepository sectionRepository;
    private final MemoryAccessService accessService;
    private final MemoryContentSafetyService contentSafetyService;
    private final TemplateSectionContractService contractService;
    private final Clock clock;

    public MemorySectionService(
            MemorySectionRepository sectionRepository,
            MemoryAccessService accessService,
            MemoryContentSafetyService contentSafetyService,
            TemplateSectionContractService contractService,
            Clock clock
    ) {
        this.sectionRepository = sectionRepository;
        this.accessService = accessService;
        this.contentSafetyService = contentSafetyService;
        this.contractService = contractService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MemorySectionResponse> list(UUID memoryId) {
        Memory memory = accessService.requireView(memoryId);
        Set<String> requiredTypes = contractService.requiredSectionTypes(
                memory.getTemplateVersionId()
        );
        return sectionRepository.findAllByMemoryIdOrderBySortOrderAsc(memoryId).stream()
                .map(section -> toResponse(section, requiredTypes.contains(section.getSectionType())))
                .toList();
    }

    @Transactional
    public MemorySectionResponse create(UUID memoryId, CreateMemorySectionRequest request) {
        Memory memory = accessService.requireEditable(memoryId);
        requireSafe(request.sectionKey(), request.title(), request.contentText());
        TemplateSectionContractCheckResponse contract = requireContract(
                memory,
                request.sectionType(),
                request.config()
        );
        UUID actorId = accessService.actorId();
        Instant now = clock.instant();
        MemorySection section = new MemorySection(
                UUID.randomUUID(),
                memoryId,
                request.sectionKey(),
                request.sectionType(),
                request.title(),
                request.contentText(),
                request.config().deepCopy(),
                request.sortOrder(),
                request.visible(),
                actorId,
                now
        );
        try {
            sectionRepository.saveAndFlush(section);
        } catch (DataIntegrityViolationException exception) {
            throw new MemorySectionConflictException();
        }
        return toResponse(section, contract.required());
    }

    @Transactional
    public MemorySectionResponse update(
            UUID memoryId,
            UUID sectionId,
            UpdateMemorySectionRequest request
    ) {
        Memory memory = accessService.requireEditable(memoryId);
        MemorySection section = find(memoryId, sectionId);
        requireVersion(section, request.version());
        requireSafe(request.title(), request.contentText());
        TemplateSectionContractCheckResponse contract = requireContract(
                memory,
                request.sectionType(),
                request.config()
        );
        section.update(
                request.sectionType(),
                request.title(),
                request.contentText(),
                request.config().deepCopy(),
                request.visible(),
                accessService.actorId(),
                clock.instant()
        );
        flushForUpdate();
        return toResponse(section, contract.required());
    }

    @Transactional
    public void delete(UUID memoryId, UUID sectionId, long version) {
        accessService.requireEditable(memoryId);
        MemorySection section = find(memoryId, sectionId);
        requireVersion(section, version);
        sectionRepository.delete(section);
        flushForUpdate();
    }

    @Transactional
    public List<MemorySectionResponse> reorder(
            UUID memoryId,
            ReorderMemoryItemsRequest request
    ) {
        Memory memory = accessService.requireEditable(memoryId);
        List<MemorySection> sections = sectionRepository.findAllByMemoryIdOrderBySortOrderAsc(
                memoryId
        );
        validateOrder(sections, request);
        Set<String> requiredTypes = contractService.requiredSectionTypes(
                memory.getTemplateVersionId()
        );
        List<UUID> currentOrder = sections.stream().map(MemorySection::getId).toList();
        if (currentOrder.equals(request.orderedIds())) {
            return responses(sections, requiredTypes);
        }

        int temporaryBase = temporaryBase(
                sections.stream().mapToInt(MemorySection::getSortOrder).max().orElse(0),
                sections.size()
        );
        UUID actorId = accessService.actorId();
        Instant now = clock.instant();
        Map<UUID, MemorySection> sectionsById = sections.stream().collect(
                java.util.stream.Collectors.toMap(MemorySection::getId, section -> section)
        );
        for (int index = 0; index < request.orderedIds().size(); index++) {
            sectionsById.get(request.orderedIds().get(index))
                    .reorder(temporaryBase + index, actorId, now);
        }
        flushForUpdate();
        for (int index = 0; index < request.orderedIds().size(); index++) {
            sectionsById.get(request.orderedIds().get(index)).reorder(index, actorId, now);
        }
        flushForUpdate();
        sections.sort(Comparator.comparingInt(MemorySection::getSortOrder));
        return responses(sections, requiredTypes);
    }

    private TemplateSectionContractCheckResponse requireContract(
            Memory memory,
            String sectionType,
            com.fasterxml.jackson.databind.JsonNode config
    ) {
        TemplateSectionContractCheckResponse contract = contractService.check(
                memory.getTemplateVersionId(),
                sectionType,
                config
        );
        if (!contract.allowed() || !contract.configValid()) {
            throw new InvalidMemorySectionContractException();
        }
        return contract;
    }

    private void validateOrder(
            List<MemorySection> sections,
            ReorderMemoryItemsRequest request
    ) {
        Set<UUID> entityIds = sections.stream()
                .map(MemorySection::getId)
                .collect(java.util.stream.Collectors.toSet());
        Set<UUID> orderedIds = new HashSet<>(request.orderedIds());
        if (orderedIds.size() != request.orderedIds().size()
                || !orderedIds.equals(entityIds)
                || !request.versions().keySet().equals(entityIds)) {
            throw new InvalidMemoryItemOrderException();
        }
        sections.forEach(section -> requireVersion(
                section,
                request.versions().get(section.getId())
        ));
    }

    private int temporaryBase(int maximumSortOrder, int itemCount) {
        long base = (long) maximumSortOrder + itemCount + 1;
        if (base + itemCount > Integer.MAX_VALUE) {
            throw new InvalidMemoryItemOrderException();
        }
        return (int) base;
    }

    private MemorySection find(UUID memoryId, UUID sectionId) {
        return sectionRepository.findByIdAndMemoryId(sectionId, memoryId)
                .orElseThrow(MemorySectionNotFoundException::new);
    }

    private void requireVersion(MemorySection section, Long expectedVersion) {
        if (expectedVersion == null || section.getVersion() != expectedVersion) {
            throw new MemoryVersionConflictException(section.getVersion());
        }
    }

    private void requireSafe(String... values) {
        for (String value : values) {
            contentSafetyService.requireSafeMarkdown(value);
        }
    }

    private void flushForUpdate() {
        try {
            sectionRepository.flush();
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new MemoryVersionConflictException(null);
        } catch (DataIntegrityViolationException exception) {
            throw new MemorySectionConflictException();
        }
    }

    private List<MemorySectionResponse> responses(
            List<MemorySection> sections,
            Set<String> requiredTypes
    ) {
        return sections.stream()
                .map(section -> toResponse(
                        section,
                        requiredTypes.contains(section.getSectionType())
                ))
                .toList();
    }

    private MemorySectionResponse toResponse(MemorySection section, boolean required) {
        return new MemorySectionResponse(
                section.getId(),
                section.getSectionKey(),
                section.getSectionType(),
                section.getTitle(),
                section.getContentText(),
                section.getConfig().deepCopy(),
                section.getSortOrder(),
                section.isVisible(),
                required,
                section.hasContent(),
                section.getCreatedAt(),
                section.getUpdatedAt(),
                section.getVersion()
        );
    }
}
