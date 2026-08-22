--liquibase formatted sql

--changeset memories:006-template-catalog
CREATE TABLE templates (
    id UUID PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    memory_type VARCHAR(30) NOT NULL,
    description VARCHAR(1000),
    thumbnail_asset_id UUID,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_templates_memory_type CHECK (
        memory_type IN ('WEDDING', 'FUNERAL', 'GRADUATION', 'HOUSEWARMING', 'PERSONAL')
    ),
    CONSTRAINT ck_templates_status CHECK (
        status IN ('DRAFT', 'ACTIVE', 'INACTIVE', 'ARCHIVED')
    )
);

CREATE INDEX ix_templates_memory_type_status
    ON templates (memory_type, status);

CREATE TABLE template_versions (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES templates(id) ON DELETE RESTRICT,
    version_no INTEGER NOT NULL,
    component_key VARCHAR(150) NOT NULL,
    renderer_version VARCHAR(50) NOT NULL,
    config_schema JSONB NOT NULL,
    default_config JSONB NOT NULL,
    required_sections JSONB NOT NULL,
    status VARCHAR(30) NOT NULL,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ux_template_versions_number UNIQUE (template_id, version_no),
    CONSTRAINT ck_template_versions_number CHECK (version_no > 0),
    CONSTRAINT ck_template_versions_status CHECK (
        status IN ('DRAFT', 'PUBLISHED', 'DEPRECATED')
    ),
    CONSTRAINT ck_template_versions_config_schema CHECK (
        jsonb_typeof(config_schema) = 'object'
    ),
    CONSTRAINT ck_template_versions_default_config CHECK (
        jsonb_typeof(default_config) = 'object'
    ),
    CONSTRAINT ck_template_versions_required_sections CHECK (
        jsonb_typeof(required_sections) = 'array'
    ),
    CONSTRAINT ck_template_versions_published_at CHECK (
        (status = 'DRAFT' AND published_at IS NULL)
        OR (status IN ('PUBLISHED', 'DEPRECATED') AND published_at IS NOT NULL)
    )
);

CREATE INDEX ix_template_versions_component_key
    ON template_versions (component_key);

-- thumbnail_asset_id receives its foreign key when media_assets is introduced.

--rollback DROP TABLE template_versions;
--rollback DROP TABLE templates;
