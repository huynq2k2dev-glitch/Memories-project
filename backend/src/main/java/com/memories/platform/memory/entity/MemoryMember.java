package com.memories.platform.memory.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "memory_members")
public class MemoryMember {

    @Id
    private UUID id;

    @Column(name = "memory_id", nullable = false, updatable = false)
    private UUID memoryId;

    @Column(name = "role_code", nullable = false, length = 50)
    private String roleCode;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "display_name", length = 150)
    private String displayName;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "avatar_asset_id")
    private UUID avatarAssetId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Version
    @Column(nullable = false)
    private long version;

    protected MemoryMember() {
    }

    public MemoryMember(
            UUID id,
            UUID memoryId,
            String roleCode,
            String fullName,
            String displayName,
            String description,
            int sortOrder,
            JsonNode metadata,
            UUID actorId,
            Instant now
    ) {
        this.id = id;
        this.memoryId = memoryId;
        this.roleCode = roleCode;
        this.fullName = fullName;
        this.displayName = displayName;
        this.description = description;
        this.sortOrder = sortOrder;
        this.metadata = metadata;
        this.createdAt = now;
        this.createdBy = actorId;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public void update(
            String roleCode,
            String fullName,
            String displayName,
            String description,
            UUID actorId,
            Instant now
    ) {
        this.roleCode = roleCode;
        this.fullName = fullName;
        this.displayName = displayName;
        this.description = description;
        this.updatedBy = actorId;
        this.updatedAt = now;
    }

    public void reorder(int sortOrder, UUID actorId, Instant now) {
        this.sortOrder = sortOrder;
        this.updatedBy = actorId;
        this.updatedAt = now;
    }

    public void updateAvatar(UUID avatarAssetId, UUID actorId, Instant now) {
        this.avatarAssetId = avatarAssetId;
        this.updatedBy = actorId;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMemoryId() {
        return memoryId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public UUID getAvatarAssetId() {
        return avatarAssetId;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public JsonNode getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
