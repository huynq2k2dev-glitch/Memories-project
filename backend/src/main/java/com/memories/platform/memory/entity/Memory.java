package com.memories.platform.memory.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.memories.platform.common.domain.MemoryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "memories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Memory {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "template_version_id", nullable = false, updatable = false)
    private UUID templateVersionId;

    @Column(nullable = false, length = 180)
    private String slug;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "memory_type", nullable = false, length = 30, updatable = false)
    private MemoryType memoryType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MemoryStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MemoryVisibility visibility;

    @Column(name = "access_password_hash", length = 255)
    private String accessPasswordHash;

    @Column(length = 1000)
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "theme_config", nullable = false, columnDefinition = "jsonb")
    private JsonNode themeConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode settings;

    @Column(name = "cover_asset_id")
    private UUID coverAssetId;

    @Column(name = "event_start_at")
    private Instant eventStartAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

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

    public Memory(
            UUID id,
            UUID ownerId,
            UUID templateVersionId,
            String slug,
            String title,
            MemoryType memoryType,
            JsonNode themeConfig,
            JsonNode settings,
            Instant now
    ) {
        this.id = id;
        this.ownerId = ownerId;
        this.templateVersionId = templateVersionId;
        this.slug = slug;
        this.title = title;
        this.memoryType = memoryType;
        this.status = MemoryStatus.DRAFT;
        this.visibility = MemoryVisibility.PRIVATE;
        this.themeConfig = themeConfig;
        this.settings = settings;
        this.createdAt = now;
        this.createdBy = ownerId;
        this.updatedAt = now;
        this.updatedBy = ownerId;
    }

    public void updateDraft(
            String title,
            String summary,
            MemoryVisibility visibility,
            String accessPasswordHash,
            JsonNode themeConfig,
            Instant eventStartAt,
            Instant expiresAt,
            UUID actorId,
            Instant now
    ) {
        this.title = title;
        this.summary = summary;
        this.visibility = visibility;
        this.accessPasswordHash = accessPasswordHash;
        this.themeConfig = themeConfig;
        this.eventStartAt = eventStartAt;
        this.expiresAt = expiresAt;
        this.updatedBy = actorId;
        this.updatedAt = now;
    }

    public void updateCover(UUID coverAssetId, UUID actorId, Instant now) {
        this.coverAssetId = coverAssetId;
        this.updatedBy = actorId;
        this.updatedAt = now;
    }

    public void updateSettings(JsonNode settings, UUID actorId, Instant now) {
        this.settings = settings;
        this.updatedBy = actorId;
        this.updatedAt = now;
    }

    public void publish(UUID actorId, Instant now) {
        this.status = MemoryStatus.PUBLISHED;
        this.publishedAt = now;
        this.updatedBy = actorId;
        this.updatedAt = now;
    }

    public void archive(UUID actorId, Instant now) {
        this.status = MemoryStatus.ARCHIVED;
        this.updatedBy = actorId;
        this.updatedAt = now;
    }

    public void softDelete(UUID actorId, Instant now) {
        this.deletedAt = now;
        this.updatedBy = actorId;
        this.updatedAt = now;
    }

    public boolean isDraft() {
        return status == MemoryStatus.DRAFT;
    }
}
