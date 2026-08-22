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
        save(actorUserId, action, entityType, entityId, result, correlationId, reasonCode);
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
        save(actorUserId, action, entityType, entityId, result, correlationId, reasonCode);
    }

    private void save(
            UUID actorUserId,
            String action,
            String entityType,
            UUID entityId,
            AuditResult result,
            String correlationId,
            String reasonCode
    ) {
        auditLogRepository.save(new AuditLog(
                UUID.randomUUID(),
                actorUserId,
                action,
                entityType,
                entityId,
                result.name(),
                correlationId,
                reasonCode == null
                        ? objectMapper.createObjectNode()
                        : objectMapper.valueToTree(Map.of("reasonCode", reasonCode)),
                clock.instant()
        ));
    }
}
