package com.memories.platform.memory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "memory_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemoryImage {

    @Id
    private UUID id;

    @Column(name = "memory_id", nullable = false, updatable = false)
    private UUID memoryId;

    @Column(name = "media_asset_id", nullable = false, updatable = false)
    private UUID mediaAssetId;

    @Column(name = "section_id")
    private UUID sectionId;

    @Column(length = 1000)
    private String caption;

    @Column(name = "alt_text", length = 500)
    private String altText;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "cover_candidate", nullable = false)
    private boolean coverCandidate;

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

    public MemoryImage(
            UUID id,
            UUID memoryId,
            UUID mediaAssetId,
            UUID sectionId,
            String caption,
            String altText,
            int sortOrder,
            boolean coverCandidate,
            UUID actorId,
            Instant now
    ) {
        this.id = id;
        this.memoryId = memoryId;
        this.mediaAssetId = mediaAssetId;
        this.sectionId = sectionId;
        this.caption = caption;
        this.altText = altText;
        this.sortOrder = sortOrder;
        this.coverCandidate = coverCandidate;
        this.createdAt = now;
        this.createdBy = actorId;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public void update(
            UUID sectionId,
            String caption,
            String altText,
            boolean coverCandidate,
            UUID actorId,
            Instant now
    ) {
        this.sectionId = sectionId;
        this.caption = caption;
        this.altText = altText;
        this.coverCandidate = coverCandidate;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public void reorder(int sortOrder, UUID actorId, Instant now) {
        this.sortOrder = sortOrder;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }
}
