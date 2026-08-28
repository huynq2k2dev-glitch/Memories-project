package com.memories.platform.template.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "template_versions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TemplateVersion {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false, updatable = false)
    private Template template;

    @Column(name = "version_no", nullable = false, updatable = false)
    private int versionNo;

    @Column(name = "component_key", nullable = false, length = 150)
    private String componentKey;

    @Column(name = "renderer_version", nullable = false, length = 50)
    private String rendererVersion;

    @Column(name = "cover_required", nullable = false)
    private boolean coverRequired;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_schema", nullable = false, columnDefinition = "jsonb")
    private JsonNode configSchema;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "default_config", nullable = false, columnDefinition = "jsonb")
    private JsonNode defaultConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "required_sections", nullable = false, columnDefinition = "jsonb")
    private JsonNode requiredSections;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "section_contracts", nullable = false, columnDefinition = "jsonb")
    private JsonNode sectionContracts;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TemplateVersionStatus status;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public TemplateVersion(
            UUID id,
            Template template,
            int versionNo,
            String componentKey,
            String rendererVersion,
            boolean coverRequired,
            JsonNode configSchema,
            JsonNode defaultConfig,
            JsonNode requiredSections,
            JsonNode sectionContracts,
            Instant now
    ) {
        this.id = id;
        this.template = template;
        this.versionNo = versionNo;
        this.componentKey = componentKey;
        this.rendererVersion = rendererVersion;
        this.coverRequired = coverRequired;
        this.configSchema = configSchema;
        this.defaultConfig = defaultConfig;
        this.requiredSections = requiredSections;
        this.sectionContracts = sectionContracts;
        this.status = TemplateVersionStatus.DRAFT;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateDraft(
            String componentKey,
            String rendererVersion,
            boolean coverRequired,
            JsonNode configSchema,
            JsonNode defaultConfig,
            JsonNode requiredSections,
            JsonNode sectionContracts,
            Instant now
    ) {
        this.componentKey = componentKey;
        this.rendererVersion = rendererVersion;
        this.coverRequired = coverRequired;
        this.configSchema = configSchema;
        this.defaultConfig = defaultConfig;
        this.requiredSections = requiredSections;
        this.sectionContracts = sectionContracts;
        this.updatedAt = now;
    }

    public void publish(Instant now) {
        this.status = TemplateVersionStatus.PUBLISHED;
        this.publishedAt = now;
        this.updatedAt = now;
    }

    public void deprecate(Instant now) {
        this.status = TemplateVersionStatus.DEPRECATED;
        this.updatedAt = now;
    }

    public boolean isDraft() {
        return status == TemplateVersionStatus.DRAFT;
    }

    public boolean isPublished() {
        return status == TemplateVersionStatus.PUBLISHED;
    }
}
