package com.memories.platform.audit.repository;

import com.memories.platform.audit.entity.AuditLog;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface AuditLogRepository extends Repository<AuditLog, UUID> {

    AuditLog save(AuditLog auditLog);
}
