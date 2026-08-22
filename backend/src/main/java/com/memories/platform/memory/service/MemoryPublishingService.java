package com.memories.platform.memory.service;

import com.memories.platform.audit.dto.AuditResult;
import com.memories.platform.audit.service.AuditLogService;
import com.memories.platform.auth.service.CurrentActorService;
import com.memories.platform.memory.constants.MemoryPublishingConstants;
import com.memories.platform.memory.dto.PublishMemoryRequest;
import com.memories.platform.memory.dto.PublishMemoryResponse;
import com.memories.platform.memory.exception.InvalidMemorySectionContractException;
import com.memories.platform.memory.exception.InvalidMemoryThemeException;
import com.memories.platform.memory.exception.MemoryNotEditableException;
import com.memories.platform.memory.exception.MemoryNotFoundException;
import com.memories.platform.memory.exception.MemoryPublishValidationException;
import com.memories.platform.memory.exception.MemoryVersionConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MemoryPublishingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryPublishingService.class);

    private final CurrentActorService currentActorService;
    private final MemoryPublishingPersistenceService persistenceService;
    private final AuditLogService auditLogService;

    public MemoryPublishingService(
            CurrentActorService currentActorService,
            MemoryPublishingPersistenceService persistenceService,
            AuditLogService auditLogService
    ) {
        this.currentActorService = currentActorService;
        this.persistenceService = persistenceService;
        this.auditLogService = auditLogService;
    }

    public PublishMemoryResponse publish(
            UUID memoryId,
            PublishMemoryRequest request,
            String correlationId
    ) {
        UUID actorId = currentActorService.userId();
        try {
            return persistenceService.publish(memoryId, actorId, request, correlationId);
        } catch (RuntimeException exception) {
            AuditResult result = exception instanceof MemoryNotFoundException
                    ? AuditResult.DENIED
                    : AuditResult.FAILURE;
            try {
                auditLogService.recordIsolated(
                        actorId,
                        MemoryPublishingConstants.AUDIT_ACTION,
                        MemoryPublishingConstants.AUDIT_ENTITY_TYPE,
                        memoryId,
                        result,
                        correlationId,
                        reasonCode(exception)
                );
            } catch (RuntimeException auditException) {
                exception.addSuppressed(auditException);
                LOGGER.error(
                        "Could not persist memory publish failure audit: {}",
                        auditException.getClass().getName()
                );
            }
            throw exception;
        }
    }

    private String reasonCode(RuntimeException exception) {
        if (exception instanceof MemoryPublishValidationException validationException) {
            return validationException.getCode();
        }
        if (exception instanceof MemoryNotFoundException) {
            return "MEMORY_NOT_FOUND";
        }
        if (exception instanceof MemoryVersionConflictException) {
            return "MEMORY_VERSION_CONFLICT";
        }
        if (exception instanceof MemoryNotEditableException) {
            return "MEMORY_NOT_EDITABLE";
        }
        if (exception instanceof InvalidMemoryThemeException) {
            return "MEMORY_THEME_INVALID";
        }
        if (exception instanceof InvalidMemorySectionContractException) {
            return "MEMORY_SECTION_CONTRACT_INVALID";
        }
        return "MEMORY_PUBLISH_FAILED";
    }
}
