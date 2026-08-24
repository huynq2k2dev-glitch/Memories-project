package com.memories.platform.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memories.platform.audit.dto.AuditResult;
import com.memories.platform.audit.entity.AuditLog;
import com.memories.platform.audit.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(
            UUID actorUserId,
            String action,
            String entityType,
            UUID entityId,
            AuditResult result,
            String correlationId,
            String reasonCode
    ) {
        recordWithMetadata(
                actorUserId,
                action,
                entityType,
                entityId,
                result,
                correlationId,
                reasonCode == null ? Map.of() : Map.of("reasonCode", reasonCode)
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordWithMetadata(
            UUID actorUserId,
            String action,
            String entityType,
            UUID entityId,
            AuditResult result,
            String correlationId,
            Map<String, ?> metadata
    ) {
        save(actorUserId, action, entityType, entityId, result, correlationId, metadata);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordIsolated(
            UUID actorUserId,
            String action,
            String entityType,
            UUID entityId,
            AuditResult result,
            String correlationId,
            String reasonCode
    ) {
        recordIsolatedWithMetadata(
                actorUserId,
                action,
                entityType,
                entityId,
                result,
                correlationId,
                reasonCode == null ? Map.of() : Map.of("reasonCode", reasonCode)
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordIsolatedWithMetadata(
            UUID actorUserId,
            String action,
            String entityType,
            UUID entityId,
            AuditResult result,
            String correlationId,
            Map<String, ?> metadata
    ) {
        save(actorUserId, action, entityType, entityId, result, correlationId, metadata);
    }

    private void save(
            UUID actorUserId,
            String action,
            String entityType,
            UUID entityId,
            AuditResult result,
            String correlationId,
            Map<String, ?> metadata
    ) {
        auditLogRepository.save(new AuditLog(
                UUID.randomUUID(),
                actorUserId,
                action,
                entityType,
                entityId,
                result.name(),
                correlationId,
                metadata == null
                        ? objectMapper.createObjectNode()
                        : objectMapper.valueToTree(metadata),
                clock.instant()
        ));
    }
}
