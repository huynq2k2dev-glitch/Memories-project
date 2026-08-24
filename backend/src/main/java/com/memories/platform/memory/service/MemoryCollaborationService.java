package com.memories.platform.memory.service;

import com.memories.platform.audit.dto.AuditResult;
import com.memories.platform.audit.service.AuditLogService;
import com.memories.platform.auth.exception.PermissionDeniedException;
import com.memories.platform.auth.service.CurrentActorService;
import com.memories.platform.memory.constants.MemoryCollaboratorConstants;
import com.memories.platform.memory.dto.AddMemoryCollaboratorRequest;
import com.memories.platform.memory.dto.MemoryCollaboratorResponse;
import com.memories.platform.memory.dto.UpdateMemoryCollaboratorRequest;
import com.memories.platform.memory.exception.MemoryNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MemoryCollaborationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryCollaborationService.class);

    private final CurrentActorService currentActorService;
    private final MemoryCollaborationPersistenceService persistenceService;
    private final AuditLogService auditLogService;

    public MemoryCollaborationService(
            CurrentActorService currentActorService,
            MemoryCollaborationPersistenceService persistenceService,
            AuditLogService auditLogService
    ) {
        this.currentActorService = currentActorService;
        this.persistenceService = persistenceService;
        this.auditLogService = auditLogService;
    }

    public List<MemoryCollaboratorResponse> list(UUID memoryId) {
        return persistenceService.list(memoryId);
    }

    public MemoryCollaboratorResponse add(
            UUID memoryId,
            AddMemoryCollaboratorRequest request,
            String correlationId
    ) {
        UUID actorId = currentActorService.userId();
        try {
            return persistenceService.add(memoryId, actorId, request, correlationId);
        } catch (RuntimeException exception) {
            auditFailure(
                    actorId,
                    MemoryCollaboratorConstants.AUDIT_ADD_ACTION,
                    memoryId,
                    memoryId,
                    correlationId,
                    exception
            );
            throw exception;
        }
    }

    public MemoryCollaboratorResponse changePermission(
            UUID memoryId,
            UUID collaboratorId,
            UpdateMemoryCollaboratorRequest request,
            String correlationId
    ) {
        UUID actorId = currentActorService.userId();
        try {
            return persistenceService.changePermission(
                    memoryId,
                    collaboratorId,
                    actorId,
                    request,
                    correlationId
            );
        } catch (RuntimeException exception) {
            auditFailure(
                    actorId,
                    MemoryCollaboratorConstants.AUDIT_PERMISSION_ACTION,
                    collaboratorId,
                    memoryId,
                    correlationId,
                    exception
            );
            throw exception;
        }
    }

    public void revoke(UUID memoryId, UUID collaboratorId, String correlationId) {
        UUID actorId = currentActorService.userId();
        try {
            persistenceService.revoke(memoryId, collaboratorId, actorId, correlationId);
        } catch (RuntimeException exception) {
            auditFailure(
                    actorId,
                    MemoryCollaboratorConstants.AUDIT_REVOKE_ACTION,
                    collaboratorId,
                    memoryId,
                    correlationId,
                    exception
            );
            throw exception;
        }
    }

    private void auditFailure(
            UUID actorId,
            String action,
            UUID entityId,
            UUID memoryId,
            String correlationId,
            RuntimeException exception
    ) {
        AuditResult result = exception instanceof MemoryNotFoundException
                || exception instanceof PermissionDeniedException
                ? AuditResult.DENIED
                : AuditResult.FAILURE;
        try {
            auditLogService.recordIsolatedWithMetadata(
                    actorId,
                    action,
                    MemoryCollaboratorConstants.AUDIT_ENTITY_TYPE,
                    entityId,
                    result,
                    correlationId,
                    Map.of(
                            "memoryId", memoryId,
                            "reasonCode", exception.getClass().getSimpleName()
                    )
            );
        } catch (RuntimeException auditException) {
            exception.addSuppressed(auditException);
            LOGGER.error(
                    "Could not persist memory collaborator failure audit: {}",
                    auditException.getClass().getName()
            );
        }
    }
}
