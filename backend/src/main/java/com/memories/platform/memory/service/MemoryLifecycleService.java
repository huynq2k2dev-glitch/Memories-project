package com.memories.platform.memory.service;

import com.memories.platform.audit.dto.AuditResult;
import com.memories.platform.audit.service.AuditLogService;
import com.memories.platform.auth.exception.PermissionDeniedException;
import com.memories.platform.auth.service.CurrentActorService;
import com.memories.platform.memory.constants.MemoryLifecycleConstants;
import com.memories.platform.memory.dto.ArchiveMemoryRequest;
import com.memories.platform.memory.dto.MemoryLifecycleResponse;
import com.memories.platform.memory.exception.MemoryLifecycleConflictException;
import com.memories.platform.memory.exception.MemoryNotFoundException;
import com.memories.platform.memory.exception.MemoryVersionConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MemoryLifecycleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryLifecycleService.class);

    private final CurrentActorService currentActorService;
    private final MemoryLifecyclePersistenceService persistenceService;
    private final AuditLogService auditLogService;

    public MemoryLifecycleService(
            CurrentActorService currentActorService,
            MemoryLifecyclePersistenceService persistenceService,
            AuditLogService auditLogService
    ) {
        this.currentActorService = currentActorService;
        this.persistenceService = persistenceService;
        this.auditLogService = auditLogService;
    }

    public MemoryLifecycleResponse archive(
            UUID memoryId,
            ArchiveMemoryRequest request,
            String correlationId
    ) {
        UUID actorId = currentActorService.userId();
        try {
            return persistenceService.archive(memoryId, actorId, request, correlationId);
        } catch (RuntimeException exception) {
            recordFailure(
                    actorId,
                    memoryId,
                    MemoryLifecycleConstants.AUDIT_ARCHIVE_ACTION,
                    "MEMORY_ARCHIVE_DENIED",
                    "MEMORY_ARCHIVE_FAILED",
                    correlationId,
                    exception
            );
            throw exception;
        }
    }

    public void softDelete(UUID memoryId, long version, String correlationId) {
        UUID actorId = currentActorService.userId();
        try {
            persistenceService.softDelete(memoryId, actorId, version, correlationId);
        } catch (RuntimeException exception) {
            recordFailure(
                    actorId,
                    memoryId,
                    MemoryLifecycleConstants.AUDIT_DELETE_ACTION,
                    "MEMORY_DELETE_DENIED",
                    "MEMORY_DELETE_FAILED",
                    correlationId,
                    exception
            );
            throw exception;
        }
    }

    private void recordFailure(
            UUID actorId,
            UUID memoryId,
            String action,
            String deniedCode,
            String failureCode,
            String correlationId,
            RuntimeException exception
    ) {
        AuditResult result = exception instanceof MemoryNotFoundException
                || exception instanceof PermissionDeniedException
                ? AuditResult.DENIED
                : AuditResult.FAILURE;
        try {
            auditLogService.recordIsolated(
                    actorId,
                    action,
                    MemoryLifecycleConstants.AUDIT_ENTITY_TYPE,
                    memoryId,
                    result,
                    correlationId,
                    reasonCode(exception, deniedCode, failureCode)
            );
        } catch (RuntimeException auditException) {
            exception.addSuppressed(auditException);
            LOGGER.error(
                    "Could not persist memory lifecycle failure audit: {}",
                    auditException.getClass().getName()
            );
        }
    }

    private String reasonCode(
            RuntimeException exception,
            String deniedCode,
            String failureCode
    ) {
        if (exception instanceof MemoryNotFoundException) {
            return "MEMORY_NOT_FOUND";
        }
        if (exception instanceof PermissionDeniedException) {
            return deniedCode;
        }
        if (exception instanceof MemoryVersionConflictException) {
            return "MEMORY_VERSION_CONFLICT";
        }
        if (exception instanceof MemoryLifecycleConflictException) {
            return "MEMORY_LIFECYCLE_CONFLICT";
        }
        return failureCode;
    }
}
