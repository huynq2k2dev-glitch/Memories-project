--liquibase formatted sql

--changeset memories:007-memory-core
CREATE TABLE memories (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    template_version_id UUID NOT NULL REFERENCES template_versions(id) ON DELETE RESTRICT,
    slug VARCHAR(180) NOT NULL,
    title VARCHAR(255) NOT NULL,
    memory_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    visibility VARCHAR(30) NOT NULL,
    access_password_hash VARCHAR(255),
    summary VARCHAR(1000),
    theme_config JSONB NOT NULL DEFAULT '{}'::jsonb,
    settings JSONB NOT NULL DEFAULT '{}'::jsonb,
    cover_asset_id UUID,
    event_start_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_memories_memory_type CHECK (
        memory_type IN ('WEDDING', 'FUNERAL', 'GRADUATION', 'HOUSEWARMING', 'PERSONAL')
    ),
    CONSTRAINT ck_memories_status CHECK (
        status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')
    ),
    CONSTRAINT ck_memories_visibility CHECK (
        visibility IN ('PUBLIC', 'UNLISTED', 'PRIVATE', 'PASSWORD_PROTECTED')
    ),
    CONSTRAINT ck_memories_access_password CHECK (
        (visibility = 'PASSWORD_PROTECTED' AND access_password_hash IS NOT NULL)
        OR (visibility <> 'PASSWORD_PROTECTED' AND access_password_hash IS NULL)
    ),
    CONSTRAINT ck_memories_theme_config CHECK (jsonb_typeof(theme_config) = 'object'),
    CONSTRAINT ck_memories_settings CHECK (jsonb_typeof(settings) = 'object')
);

CREATE UNIQUE INDEX ux_memories_slug_active
    ON memories (LOWER(slug))
    WHERE deleted_at IS NULL;

CREATE INDEX ix_memories_owner_status
    ON memories (owner_id, status);

CREATE INDEX ix_memories_status_visibility_published
    ON memories (status, visibility, published_at);

-- cover_asset_id receives its foreign key when media_assets is introduced.

--rollback DROP TABLE memories;
