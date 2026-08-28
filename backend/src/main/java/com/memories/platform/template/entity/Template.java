package com.memories.platform.template.entity;

import com.memories.platform.common.domain.MemoryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Template {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 100, updatable = false)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "memory_type", nullable = false, length = 30)
    private MemoryType memoryType;

    @Column(length = 1000)
    private String description;

    @Column(name = "thumbnail_asset_id")
    private UUID thumbnailAssetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TemplateStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @OneToMany(mappedBy = "template")
    @OrderBy("versionNo DESC")
    private List<TemplateVersion> versions = new ArrayList<>();

    public Template(
            UUID id,
            String code,
            String name,
            MemoryType memoryType,
            String description,
            Instant now
    ) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.memoryType = memoryType;
        this.description = description;
        this.status = TemplateStatus.DRAFT;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateMetadata(
            String name,
            MemoryType memoryType,
            String description,
            TemplateStatus status,
            Instant now
    ) {
        this.name = name;
        this.memoryType = memoryType;
        this.description = description;
        this.status = status;
        this.updatedAt = now;
    }

    public List<TemplateVersion> getVersions() {
        return List.copyOf(versions);
    }
}
