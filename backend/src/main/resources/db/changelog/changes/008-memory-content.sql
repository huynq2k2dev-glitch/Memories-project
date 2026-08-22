--liquibase formatted sql

--changeset memories:008-memory-content
ALTER TABLE template_versions
    ADD COLUMN section_contracts JSONB NOT NULL DEFAULT '{}'::jsonb;

UPDATE template_versions template_version
SET section_contracts = COALESCE(
    (
        SELECT jsonb_object_agg(
            section_type,
            jsonb_build_object(
                'configSchema',
                '{"type":"object"}'::jsonb
            )
        )
        FROM jsonb_array_elements_text(template_version.required_sections) section_type
    ),
    '{}'::jsonb
);

ALTER TABLE template_versions
    ADD CONSTRAINT ck_template_versions_section_contracts
        CHECK (jsonb_typeof(section_contracts) = 'object');

CREATE TABLE memory_members (
    id UUID PRIMARY KEY,
    memory_id UUID NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    role_code VARCHAR(50) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    display_name VARCHAR(150),
    description TEXT,
    avatar_asset_id UUID,
    sort_order INTEGER NOT NULL DEFAULT 0,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ux_memory_members_role_order UNIQUE (memory_id, role_code, sort_order),
    CONSTRAINT ck_memory_members_role_code CHECK (
        role_code ~ '^[A-Z][A-Z0-9_]{0,49}$'
    ),
    CONSTRAINT ck_memory_members_sort_order CHECK (sort_order >= 0),
    CONSTRAINT ck_memory_members_metadata CHECK (jsonb_typeof(metadata) = 'object')
);

CREATE INDEX ix_memory_members_memory_order
    ON memory_members (memory_id, sort_order);

CREATE TABLE memory_sections (
    id UUID PRIMARY KEY,
    memory_id UUID NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    section_key VARCHAR(100) NOT NULL,
    section_type VARCHAR(50) NOT NULL,
    title VARCHAR(255),
    content_text TEXT,
    config JSONB NOT NULL DEFAULT '{}'::jsonb,
    sort_order INTEGER NOT NULL,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ux_memory_sections_key UNIQUE (memory_id, section_key),
    CONSTRAINT ux_memory_sections_order UNIQUE (memory_id, sort_order),
    CONSTRAINT ck_memory_sections_type CHECK (
        section_type ~ '^[A-Z][A-Z0-9_]{0,49}$'
    ),
    CONSTRAINT ck_memory_sections_sort_order CHECK (sort_order >= 0),
    CONSTRAINT ck_memory_sections_config CHECK (jsonb_typeof(config) = 'object')
);

CREATE INDEX ix_memory_sections_memory_visibility_order
    ON memory_sections (memory_id, is_visible, sort_order);

-- avatar_asset_id receives its foreign key when media_assets is introduced.

--rollback DROP TABLE memory_sections;
--rollback DROP TABLE memory_members;
--rollback ALTER TABLE template_versions DROP COLUMN section_contracts;
