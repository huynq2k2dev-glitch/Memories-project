package com.memories.platform.memory.service;

import com.memories.platform.memory.constants.MemoryMessageConstants;
import com.memories.platform.memory.dto.GuestMessageModeratorContextResponse;
import com.memories.platform.memory.dto.GuestMessageSubmissionContextResponse;
import com.memories.platform.memory.entity.Memory;
import com.memories.platform.memory.entity.MemoryStatus;
import com.memories.platform.memory.exception.MemoryNotFoundException;
import com.memories.platform.memory.exception.MemoryPasswordRequiredException;
import com.memories.platform.memory.repository.MemoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

@Service
public class MemoryMessageAccessService {

    private final MemoryRepository memoryRepository;
    private final MemoryAccessService accessService;
    private final MemoryPasswordAccessService passwordAccessService;
    private final Clock clock;

    public MemoryMessageAccessService(
            MemoryRepository memoryRepository,
            MemoryAccessService accessService,
            MemoryPasswordAccessService passwordAccessService,
            Clock clock
    ) {
        this.memoryRepository = memoryRepository;
        this.accessService = accessService;
        this.passwordAccessService = passwordAccessService;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public GuestMessageSubmissionContextResponse requireSubmission(
            String slug,
            Map<String, String> cookies
    ) {
        Memory memory = memoryRepository.findMessageSubmissionMemoryForUpdate(
                slug,
                MemoryStatus.PUBLISHED,
                clock.instant()
        ).orElseThrow(MemoryNotFoundException::new);

        switch (memory.getVisibility()) {
            case PUBLIC, UNLISTED -> {
            }
            case PASSWORD_PROTECTED -> {
                if (!passwordAccessService.hasValidGrant(memory.getId(), cookies)) {
                    throw new MemoryPasswordRequiredException();
                }
            }
            case PRIVATE -> throw new MemoryNotFoundException();
        }

        return new GuestMessageSubmissionContextResponse(
                memory.getId(),
                memory.getSettings()
                        .path(MemoryMessageConstants.MODERATION_SETTING)
                        .asBoolean(true)
        );
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public GuestMessageModeratorContextResponse requireModerator(UUID memoryId) {
        Memory memory = accessService.requireManageCollaborators(memoryId);
        return new GuestMessageModeratorContextResponse(
                memory.getId(),
                accessService.actorId()
        );
    }
}
