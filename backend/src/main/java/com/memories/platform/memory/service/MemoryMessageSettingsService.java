package com.memories.platform.memory.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.memories.platform.audit.dto.AuditResult;
import com.memories.platform.audit.service.AuditLogService;
import com.memories.platform.memory.constants.MemoryMessageConstants;
import com.memories.platform.memory.dto.MessageModerationSettingsResponse;
import com.memories.platform.memory.dto.UpdateMessageModerationRequest;
import com.memories.platform.memory.entity.Memory;
import com.memories.platform.memory.entity.MemoryStatus;
import com.memories.platform.memory.exception.MemoryMessageSettingsConflictException;
import com.memories.platform.memory.exception.MemoryVersionConflictException;
import com.memories.platform.memory.repository.MemoryRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

@Service
public class MemoryMessageSettingsService {

    private final MemoryRepository memoryRepository;
    private final MemoryAccessService accessService;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public MemoryMessageSettingsService(
            MemoryRepository memoryRepository,
            MemoryAccessService accessService,
            AuditLogService auditLogService,
            Clock clock
    ) {
        this.memoryRepository = memoryRepository;
        this.accessService = accessService;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    @Transactional
    public MessageModerationSettingsResponse update(
            UUID memoryId,
            UpdateMessageModerationRequest request,
            String correlationId
    ) {
        Memory memory = accessService.requireManageCollaborators(memoryId);
        if (memory.getStatus() == MemoryStatus.ARCHIVED) {
            throw new MemoryMessageSettingsConflictException();
        }
        if (memory.getVersion() != request.version()) {
            throw new MemoryVersionConflictException(memory.getVersion());
        }

        ObjectNode settings = (ObjectNode) memory.getSettings().deepCopy();
        settings.put(MemoryMessageConstants.MODERATION_SETTING, request.enabled());
        UUID actorId = accessService.actorId();
        memory.updateSettings(settings, actorId, clock.instant());
        try {
            memoryRepository.flush();
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new MemoryVersionConflictException(null);
        }

        auditLogService.recordWithMetadata(
                actorId,
                MemoryMessageConstants.AUDIT_SETTING_ACTION,
                MemoryMessageConstants.AUDIT_ENTITY_TYPE,
                memoryId,
                AuditResult.SUCCESS,
                correlationId,
                Map.of("enabled", request.enabled())
        );
        return new MessageModerationSettingsResponse(
                request.enabled(),
                memory.getVersion(),
                memory.getUpdatedAt()
        );
    }
}
