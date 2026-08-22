package com.memories.platform.memory.service;

import com.memories.platform.auth.service.CurrentActorService;
import com.memories.platform.memory.entity.Memory;
import com.memories.platform.memory.exception.MemoryNotEditableException;
import com.memories.platform.memory.exception.MemoryNotFoundException;
import com.memories.platform.memory.repository.MemoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MemoryDraftAccessService {

    private final MemoryRepository memoryRepository;
    private final CurrentActorService currentActorService;

    public MemoryDraftAccessService(
            MemoryRepository memoryRepository,
            CurrentActorService currentActorService
    ) {
        this.memoryRepository = memoryRepository;
        this.currentActorService = currentActorService;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Memory requireOwned(UUID memoryId) {
        UUID ownerId = currentActorService.userId();
        return memoryRepository.findByIdAndOwnerIdAndDeletedAtIsNull(memoryId, ownerId)
                .orElseThrow(MemoryNotFoundException::new);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Memory requireEditable(UUID memoryId) {
        Memory memory = requireOwned(memoryId);
        if (!memory.isDraft()) {
            throw new MemoryNotEditableException();
        }
        return memory;
    }

    public UUID actorId() {
        return currentActorService.userId();
    }
}
