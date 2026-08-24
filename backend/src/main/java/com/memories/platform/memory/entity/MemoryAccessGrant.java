package com.memories.platform.memory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "memory_access_grants")
public class MemoryAccessGrant {

    @Id
    private UUID id;

    @Column(name = "memory_id", nullable = false, updatable = false)
    private UUID memoryId;

    @Column(
            name = "token_hash",
            nullable = false,
            length = 64,
            updatable = false,
            columnDefinition = "char(64)"
    )
    @JdbcTypeCode(SqlTypes.CHAR)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MemoryAccessGrant() {
    }

    public MemoryAccessGrant(
            UUID id,
            UUID memoryId,
            String tokenHash,
            Instant expiresAt,
            Instant createdAt
    ) {
        this.id = id;
        this.memoryId = memoryId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }
}
