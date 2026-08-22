package com.memories.platform.auth.repository;

import com.memories.platform.auth.entity.AuthAuditEvent;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface AuthAuditEventRepository extends Repository<AuthAuditEvent, UUID> {

    AuthAuditEvent save(AuthAuditEvent event);
}
