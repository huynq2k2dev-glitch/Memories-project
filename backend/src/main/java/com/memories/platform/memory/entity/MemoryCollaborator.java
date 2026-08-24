package com.memories.platform.memory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "memory_collaborators")
public class MemoryCollaborator {

    @Id
    private UUID id;

    @Column(name = "memory_id", nullable = false, updatable = false)
    private UUID memoryId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemoryCollaboratorPermission permission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemoryCollaboratorStatus status;

    @Column(name = "invited_by", nullable = false)
    private UUID invitedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected MemoryCollaborator() {
    }

    public MemoryCollaborator(
            UUID id,
            UUID memoryId,
            UUID userId,
            MemoryCollaboratorPermission permission,
            UUID invitedBy,
            Instant now
    ) {
        this.id = id;
        this.memoryId = memoryId;
        this.userId = userId;
        this.permission = permission;
        this.status = MemoryCollaboratorStatus.ACTIVE;
        this.invitedBy = invitedBy;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void changePermission(MemoryCollaboratorPermission permission, Instant now) {
        this.permission = permission;
        this.updatedAt = now;
    }

    public void reactivate(
            MemoryCollaboratorPermission permission,
            UUID invitedBy,
            Instant now
    ) {
        this.permission = permission;
        this.status = MemoryCollaboratorStatus.ACTIVE;
        this.invitedBy = invitedBy;
        this.revokedAt = null;
        this.updatedAt = now;
    }

    public void revoke(Instant now) {
        this.status = MemoryCollaboratorStatus.REVOKED;
        this.revokedAt = now;
        this.updatedAt = now;
    }

    public boolean isActive() {
        return status == MemoryCollaboratorStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMemoryId() {
        return memoryId;
    }

    public UUID getUserId() {
        return userId;
    }

    public MemoryCollaboratorPermission getPermission() {
        return permission;
    }

    public MemoryCollaboratorStatus getStatus() {
        return status;
    }

    public UUID getInvitedBy() {
        return invitedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
