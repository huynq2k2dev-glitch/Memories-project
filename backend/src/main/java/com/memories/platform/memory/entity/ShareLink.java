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
@Table(name = "share_links")
public class ShareLink {

    @Id
    private UUID id;

    @Column(name = "memory_id", nullable = false, updatable = false)
    private UUID memoryId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128, updatable = false)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private ShareLinkPermission permission;

    @Column(name = "guest_id", updatable = false)
    private UUID guestId;

    @Column(name = "expires_at", updatable = false)
    private Instant expiresAt;

    @Column(name = "max_uses", updatable = false)
    private Integer maxUses;

    @Column(name = "use_count", nullable = false)
    private int useCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShareLinkStatus status;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected ShareLink() {
    }

    public ShareLink(
            UUID id,
            UUID memoryId,
            String tokenHash,
            ShareLinkPermission permission,
            UUID guestId,
            Instant expiresAt,
            Integer maxUses,
            UUID createdBy,
            Instant createdAt
    ) {
        this.id = id;
        this.memoryId = memoryId;
        this.tokenHash = tokenHash;
        this.permission = permission;
        this.guestId = guestId;
        this.expiresAt = expiresAt;
        this.maxUses = maxUses;
        this.status = ShareLinkStatus.ACTIVE;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public void revoke(Instant now) {
        this.status = ShareLinkStatus.REVOKED;
        this.revokedAt = now;
    }

    public boolean redeem(Instant now) {
        boolean expired = expiresAt != null && !expiresAt.isAfter(now);
        boolean exhausted = maxUses != null && useCount >= maxUses;
        if (status != ShareLinkStatus.ACTIVE || expired || exhausted) {
            return false;
        }
        useCount++;
        return true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMemoryId() {
        return memoryId;
    }

    public ShareLinkPermission getPermission() {
        return permission;
    }

    public UUID getGuestId() {
        return guestId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Integer getMaxUses() {
        return maxUses;
    }

    public int getUseCount() {
        return useCount;
    }

    public ShareLinkStatus getStatus() {
        return status;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
