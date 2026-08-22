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
@Table(name = "memory_sections")
public class MemorySection {

    @Id
    private UUID id;

    @Column(name = "memory_id", nullable = false, updatable = false)
    private UUID memoryId;

    @Column(name = "section_key", nullable = false, length = 100, updatable = false)
    private String sectionKey;

    @Column(name = "section_type", nullable = false, length = 50)
    private String sectionType;

    @Column(length = 255)
    private String title;

    @Column(name = "content_text", columnDefinition = "text")
    private String contentText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode config;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_visible", nullable = false)
    private boolean visible;

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

    protected MemorySection() {
    }

    public MemorySection(
            UUID id,
            UUID memoryId,
            String sectionKey,
            String sectionType,
            String title,
            String contentText,
            JsonNode config,
            int sortOrder,
            boolean visible,
            UUID actorId,
            Instant now
    ) {
        this.id = id;
        this.memoryId = memoryId;
        this.sectionKey = sectionKey;
        this.sectionType = sectionType;
        this.title = title;
        this.contentText = contentText;
        this.config = config;
        this.sortOrder = sortOrder;
        this.visible = visible;
        this.createdAt = now;
        this.createdBy = actorId;
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public void update(
            String sectionType,
            String title,
            String contentText,
            JsonNode config,
            boolean visible,
            UUID actorId,
            Instant now
    ) {
        this.sectionType = sectionType;
        this.title = title;
        this.contentText = contentText;
        this.config = config;
        this.visible = visible;
        this.updatedBy = actorId;
        this.updatedAt = now;
    }

    public void reorder(int sortOrder, UUID actorId, Instant now) {
        this.sortOrder = sortOrder;
        this.updatedBy = actorId;
        this.updatedAt = now;
    }

    public boolean hasContent() {
        return visible
                && ((contentText != null && !contentText.isBlank()) || !config.isEmpty());
    }

    public UUID getId() {
        return id;
    }

    public UUID getMemoryId() {
        return memoryId;
    }

    public String getSectionKey() {
        return sectionKey;
    }

    public String getSectionType() {
        return sectionType;
    }

    public String getTitle() {
        return title;
    }

    public String getContentText() {
        return contentText;
    }

    public JsonNode getConfig() {
        return config;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isVisible() {
        return visible;
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
