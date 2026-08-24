package com.memories.platform.memory.service;

import com.memories.platform.audit.dto.AuditResult;
import com.memories.platform.audit.service.AuditLogService;
import com.memories.platform.memory.constants.MemoryLifecycleConstants;
import com.memories.platform.memory.dto.ArchiveMemoryRequest;
import com.memories.platform.memory.dto.MemoryLifecycleResponse;
import com.memories.platform.memory.entity.Memory;
import com.memories.platform.memory.entity.MemoryStatus;
import com.memories.platform.memory.exception.MemoryLifecycleConflictException;
import com.memories.platform.memory.exception.MemoryVersionConflictException;
import com.memories.platform.memory.repository.MemoryRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class MemoryLifecyclePersistenceService {

    private final MemoryRepository memoryRepository;
    private final MemoryAccessService accessService;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public MemoryLifecyclePersistenceService(
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
    public MemoryLifecycleResponse archive(
            UUID memoryId,
            UUID actorId,
            ArchiveMemoryRequest request,
            String correlationId
    ) {
        Memory memory = accessService.requireOwner(memoryId);
        requireVersion(memory, request.version());
        if (memory.getStatus() == MemoryStatus.ARCHIVED) {
            throw new MemoryLifecycleConflictException();
        }

        Instant now = clock.instant();
        memory.archive(actorId, now);
        flush();
        auditLogService.record(
                actorId,
                MemoryLifecycleConstants.AUDIT_ARCHIVE_ACTION,
                MemoryLifecycleConstants.AUDIT_ENTITY_TYPE,
                memoryId,
                AuditResult.SUCCESS,
                correlationId,
                null
        );
        return new MemoryLifecycleResponse(
                memory.getId(),
                memory.getStatus(),
                memory.getUpdatedAt(),
                memory.getVersion()
        );
    }

    @Transactional
    public void softDelete(
            UUID memoryId,
            UUID actorId,
            long version,
            String correlationId
    ) {
        Memory memory = accessService.requireOwner(memoryId);
        requireVersion(memory, version);
        memory.softDelete(actorId, clock.instant());
        flush();
        auditLogService.record(
                actorId,
                MemoryLifecycleConstants.AUDIT_DELETE_ACTION,
                MemoryLifecycleConstants.AUDIT_ENTITY_TYPE,
                memoryId,
                AuditResult.SUCCESS,
                correlationId,
                null
        );
    }

    private void requireVersion(Memory memory, Long version) {
        if (version == null || memory.getVersion() != version) {
            throw new MemoryVersionConflictException(memory.getVersion());
        }
    }

    private void flush() {
        try {
            memoryRepository.flush();
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new MemoryVersionConflictException(null);
        }
    }
}
