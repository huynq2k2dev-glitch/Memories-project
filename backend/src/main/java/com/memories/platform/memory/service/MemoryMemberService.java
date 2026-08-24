package com.memories.platform.memory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memories.platform.media.service.MediaAssetAccessService;
import com.memories.platform.memory.dto.CreateMemoryMemberRequest;
import com.memories.platform.memory.dto.MemoryMemberResponse;
import com.memories.platform.memory.dto.ReorderMemoryItemsRequest;
import com.memories.platform.memory.dto.UpdateMemoryMemberRequest;
import com.memories.platform.memory.dto.UpdateMemoryAssetReferenceRequest;
import com.memories.platform.memory.entity.MemoryMember;
import com.memories.platform.memory.exception.InvalidMemoryItemOrderException;
import com.memories.platform.memory.exception.MemoryMemberConflictException;
import com.memories.platform.memory.exception.MemoryMemberNotFoundException;
import com.memories.platform.memory.exception.MemoryVersionConflictException;
import com.memories.platform.memory.repository.MemoryMemberRepository;
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
public class MemoryMemberService {

    private final MemoryMemberRepository memberRepository;
    private final MemoryAccessService accessService;
    private final MemoryContentSafetyService contentSafetyService;
    private final ObjectMapper objectMapper;
    private final MediaAssetAccessService assetAccessService;
    private final Clock clock;

    public MemoryMemberService(
            MemoryMemberRepository memberRepository,
            MemoryAccessService accessService,
            MemoryContentSafetyService contentSafetyService,
            ObjectMapper objectMapper,
            MediaAssetAccessService assetAccessService,
            Clock clock
    ) {
        this.memberRepository = memberRepository;
        this.accessService = accessService;
        this.contentSafetyService = contentSafetyService;
        this.objectMapper = objectMapper;
        this.assetAccessService = assetAccessService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MemoryMemberResponse> list(UUID memoryId) {
        accessService.requireView(memoryId);
        return memberRepository.findAllByMemoryIdOrderBySortOrderAsc(memoryId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MemoryMemberResponse create(UUID memoryId, CreateMemoryMemberRequest request) {
        accessService.requireEditable(memoryId);
        requireSafe(request.fullName(), request.displayName(), request.description());
        UUID actorId = accessService.actorId();
        Instant now = clock.instant();
        MemoryMember member = new MemoryMember(
                UUID.randomUUID(),
                memoryId,
                request.roleCode(),
                request.fullName(),
                request.displayName(),
                request.description(),
                request.sortOrder(),
                objectMapper.createObjectNode(),
                actorId,
                now
        );
        try {
            memberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException exception) {
            throw new MemoryMemberConflictException();
        }
        return toResponse(member);
    }

    @Transactional
    public MemoryMemberResponse update(
            UUID memoryId,
            UUID memberId,
            UpdateMemoryMemberRequest request
    ) {
        accessService.requireEditable(memoryId);
        MemoryMember member = find(memoryId, memberId);
        requireVersion(member, request.version());
        requireSafe(request.fullName(), request.displayName(), request.description());
        member.update(
                request.roleCode(),
                request.fullName(),
                request.displayName(),
                request.description(),
                accessService.actorId(),
                clock.instant()
        );
        flushForUpdate();
        return toResponse(member);
    }

    @Transactional
    public void delete(UUID memoryId, UUID memberId, long version) {
        accessService.requireEditable(memoryId);
        MemoryMember member = find(memoryId, memberId);
        requireVersion(member, version);
        memberRepository.delete(member);
        flushForUpdate();
    }

    @Transactional
    public MemoryMemberResponse updateAvatar(
            UUID memoryId,
            UUID memberId,
            UpdateMemoryAssetReferenceRequest request
    ) {
        accessService.requireEditable(memoryId);
        MemoryMember member = find(memoryId, memberId);
        requireVersion(member, request.version());
        UUID actorId = accessService.actorId();
        if (request.assetId() != null) {
            assetAccessService.requireReadyOwned(request.assetId(), actorId);
        }
        member.updateAvatar(request.assetId(), actorId, clock.instant());
        flushForUpdate();
        return toResponse(member);
    }

    @Transactional
    public List<MemoryMemberResponse> reorder(
            UUID memoryId,
            ReorderMemoryItemsRequest request
    ) {
        accessService.requireEditable(memoryId);
        List<MemoryMember> members = memberRepository.findAllByMemoryIdOrderBySortOrderAsc(memoryId);
        validateOrder(members, request);
        List<UUID> currentOrder = members.stream().map(MemoryMember::getId).toList();
        if (currentOrder.equals(request.orderedIds())) {
            return members.stream().map(this::toResponse).toList();
        }

        int temporaryBase = temporaryBase(
                members.stream().mapToInt(MemoryMember::getSortOrder).max().orElse(0),
                members.size()
        );
        UUID actorId = accessService.actorId();
        Instant now = clock.instant();
        Map<UUID, MemoryMember> membersById = members.stream().collect(
                java.util.stream.Collectors.toMap(MemoryMember::getId, member -> member)
        );
        for (int index = 0; index < request.orderedIds().size(); index++) {
            membersById.get(request.orderedIds().get(index))
                    .reorder(temporaryBase + index, actorId, now);
        }
        flushForUpdate();
        for (int index = 0; index < request.orderedIds().size(); index++) {
            membersById.get(request.orderedIds().get(index)).reorder(index, actorId, now);
        }
        flushForUpdate();
        members.sort(Comparator.comparingInt(MemoryMember::getSortOrder));
        return members.stream().map(this::toResponse).toList();
    }

    private void validateOrder(
            List<MemoryMember> members,
            ReorderMemoryItemsRequest request
    ) {
        Set<UUID> entityIds = members.stream()
                .map(MemoryMember::getId)
                .collect(java.util.stream.Collectors.toSet());
        Set<UUID> orderedIds = new HashSet<>(request.orderedIds());
        if (orderedIds.size() != request.orderedIds().size()
                || !orderedIds.equals(entityIds)
                || !request.versions().keySet().equals(entityIds)) {
            throw new InvalidMemoryItemOrderException();
        }
        members.forEach(member -> requireVersion(member, request.versions().get(member.getId())));
    }

    private int temporaryBase(int maximumSortOrder, int itemCount) {
        long base = (long) maximumSortOrder + itemCount + 1;
        if (base + itemCount > Integer.MAX_VALUE) {
            throw new InvalidMemoryItemOrderException();
        }
        return (int) base;
    }

    private MemoryMember find(UUID memoryId, UUID memberId) {
        return memberRepository.findByIdAndMemoryId(memberId, memoryId)
                .orElseThrow(MemoryMemberNotFoundException::new);
    }

    private void requireVersion(MemoryMember member, Long expectedVersion) {
        if (expectedVersion == null || member.getVersion() != expectedVersion) {
            throw new MemoryVersionConflictException(member.getVersion());
        }
    }

    private void requireSafe(String... values) {
        for (String value : values) {
            contentSafetyService.requireSafeMarkdown(value);
        }
    }

    private void flushForUpdate() {
        try {
            memberRepository.flush();
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new MemoryVersionConflictException(null);
        } catch (DataIntegrityViolationException exception) {
            throw new MemoryMemberConflictException();
        }
    }

    private MemoryMemberResponse toResponse(MemoryMember member) {
        return new MemoryMemberResponse(
                member.getId(),
                member.getRoleCode(),
                member.getFullName(),
                member.getDisplayName(),
                member.getDescription(),
                member.getAvatarAssetId(),
                member.getSortOrder(),
                member.getCreatedAt(),
                member.getUpdatedAt(),
                member.getVersion()
        );
    }
}
