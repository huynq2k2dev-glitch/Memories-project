package com.memories.platform.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_audit_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthAuditEvent {

    @Id
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, length = 20)
    private String outcome;

    @Column(name = "reason_code", length = 50)
    private String reasonCode;

    @Column(name = "subject_user_id")
    private UUID subjectUserId;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "target_user_id")
    private UUID targetUserId;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public AuthAuditEvent(
            UUID id,
            String eventType,
            String outcome,
            String reasonCode,
            UUID subjectUserId,
            String correlationId,
            Instant occurredAt
    ) {
        this(
                id,
                eventType,
                outcome,
                reasonCode,
                subjectUserId,
                correlationId,
                occurredAt,
                null,
                null
        );
    }

    public AuthAuditEvent(
            UUID id,
            String eventType,
            String outcome,
            String reasonCode,
            UUID subjectUserId,
            String correlationId,
            Instant occurredAt,
            UUID actorUserId,
            UUID targetUserId
    ) {
        this.id = id;
        this.eventType = eventType;
        this.outcome = outcome;
        this.reasonCode = reasonCode;
        this.subjectUserId = subjectUserId;
        this.actorUserId = actorUserId;
        this.targetUserId = targetUserId;
        this.correlationId = correlationId;
        this.occurredAt = occurredAt;
    }
}
