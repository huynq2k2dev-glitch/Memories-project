package com.memories.platform.memory.service;

import com.memories.platform.auth.exception.PermissionDeniedException;
import com.memories.platform.auth.service.CurrentActorService;
import com.memories.platform.memory.dto.MemoryCapabilitiesResponse;
import com.memories.platform.memory.entity.Memory;
import com.memories.platform.memory.entity.MemoryCollaboratorPermission;
import com.memories.platform.memory.entity.MemoryCollaboratorStatus;
import com.memories.platform.memory.entity.MemoryStatus;
import com.memories.platform.memory.exception.MemoryNotEditableException;
import com.memories.platform.memory.exception.MemoryNotFoundException;
import com.memories.platform.memory.repository.MemoryCollaboratorRepository;
import com.memories.platform.memory.repository.MemoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class MemoryAccessService {

    private final MemoryRepository memoryRepository;
    private final MemoryCollaboratorRepository collaboratorRepository;
    private final CurrentActorService currentActorService;

    public MemoryAccessService(
            MemoryRepository memoryRepository,
            MemoryCollaboratorRepository collaboratorRepository,
            CurrentActorService currentActorService
    ) {
        this.memoryRepository = memoryRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.currentActorService = currentActorService;
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public Memory requireView(UUID memoryId) {
        Memory memory = memoryRepository.findByIdAndDeletedAtIsNull(memoryId)
                .orElseThrow(MemoryNotFoundException::new);
        if (!canView(memory, currentActorService.userId())) {
            throw new MemoryNotFoundException();
        }
        return memory;
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public Memory requireEditable(UUID memoryId) {
        Memory memory = requireView(memoryId);
        MemoryCapabilitiesResponse capabilities = capabilities(memory);
        if (!capabilities.canEdit()) {
            throw new PermissionDeniedException();
        }
        if (!memory.isDraft()) {
            throw new MemoryNotEditableException();
        }
        return memory;
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public Memory requirePublish(UUID memoryId) {
        Memory memory = requireView(memoryId);
        if (!capabilities(memory).canPublish()) {
            throw new PermissionDeniedException();
        }
        return memory;
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public Memory requireManageCollaborators(UUID memoryId) {
        Memory memory = requireView(memoryId);
        if (!capabilities(memory).canManageCollaborators()) {
            throw new PermissionDeniedException();
        }
        return memory;
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public Memory requireOwner(UUID memoryId) {
        Memory memory = requireView(memoryId);
        if (!Objects.equals(memory.getOwnerId(), currentActorService.userId())) {
            throw new PermissionDeniedException();
        }
        return memory;
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public MemoryCapabilitiesResponse capabilities(Memory memory) {
        UUID actorId = currentActorService.userId();
        if (Objects.equals(memory.getOwnerId(), actorId)) {
            return new MemoryCapabilitiesResponse(
                    true,
                    null,
                    true,
                    true,
                    true,
                    true,
                    true,
                    memory.getStatus() != MemoryStatus.ARCHIVED,
                    true
            );
        }

        MemoryCollaboratorPermission permission = activePermission(memory.getId(), actorId);
        boolean canEdit = permission == MemoryCollaboratorPermission.EDIT
                || permission == MemoryCollaboratorPermission.ADMIN;
        boolean admin = permission == MemoryCollaboratorPermission.ADMIN;
        return new MemoryCapabilitiesResponse(
                false,
                permission,
                canEdit,
                admin,
                admin,
                false,
                false,
                false,
                false
        );
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public boolean canView(Memory memory, UUID actorId) {
        return actorId != null && (
                Objects.equals(memory.getOwnerId(), actorId)
                        || activePermission(memory.getId(), actorId) != null
        );
    }

    public UUID actorId() {
        return currentActorService.userId();
    }

    private MemoryCollaboratorPermission activePermission(UUID memoryId, UUID actorId) {
        return collaboratorRepository.findByMemoryIdAndUserIdAndStatus(
                memoryId,
                actorId,
                MemoryCollaboratorStatus.ACTIVE
        ).map(collaborator -> collaborator.getPermission()).orElse(null);
    }
}
