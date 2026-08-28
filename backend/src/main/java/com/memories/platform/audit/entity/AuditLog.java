package com.memories.platform.audit.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    @Id
    private UUID id;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(nullable = false, length = 20)
    private String result;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "ip_hash", length = 128)
    private String ipHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AuditLog(
            UUID id,
            UUID actorUserId,
            String action,
            String entityType,
            UUID entityId,
            String result,
            String correlationId,
            JsonNode metadata,
            Instant createdAt
    ) {
        this.id = id;
        this.actorUserId = actorUserId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.result = result;
        this.correlationId = correlationId;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }
}
