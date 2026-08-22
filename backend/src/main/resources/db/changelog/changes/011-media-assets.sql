--liquibase formatted sql

--changeset memories:011-media-assets
CREATE TABLE media_assets (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    storage_provider VARCHAR(20) NOT NULL,
    bucket_name VARCHAR(100) NOT NULL,
    object_key VARCHAR(1024) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    width INTEGER,
    height INTEGER,
    checksum VARCHAR(128),
    status VARCHAR(20) NOT NULL,
    parent_asset_id UUID REFERENCES media_assets(id) ON DELETE RESTRICT,
    variant_type VARCHAR(20) NOT NULL,
    upload_expires_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ux_media_assets_storage_key UNIQUE (storage_provider, bucket_name, object_key),
    CONSTRAINT ck_media_assets_provider CHECK (storage_provider IN ('R2', 'S3', 'MINIO')),
    CONSTRAINT ck_media_assets_status CHECK (status IN ('UPLOADING', 'READY', 'FAILED', 'DELETED')),
    CONSTRAINT ck_media_assets_variant CHECK (variant_type IN ('ORIGINAL', 'LARGE', 'THUMBNAIL')),
    CONSTRAINT ck_media_assets_file_size CHECK (file_size >= 0),
    CONSTRAINT ck_media_assets_width CHECK (width IS NULL OR width > 0),
    CONSTRAINT ck_media_assets_height CHECK (height IS NULL OR height > 0),
    CONSTRAINT ck_media_assets_upload_expiry CHECK (
        (status = 'UPLOADING' AND upload_expires_at IS NOT NULL)
        OR status <> 'UPLOADING'
    )
);

CREATE INDEX ix_media_assets_owner_quota
    ON media_assets (owner_id, status, upload_expires_at);

CREATE TABLE memory_images (
    id UUID PRIMARY KEY,
    memory_id UUID NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    media_asset_id UUID NOT NULL REFERENCES media_assets(id) ON DELETE RESTRICT,
    section_id UUID REFERENCES memory_sections(id) ON DELETE SET NULL,
    caption VARCHAR(1000),
    alt_text VARCHAR(500),
    sort_order INTEGER NOT NULL,
    cover_candidate BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ux_memory_images_asset UNIQUE (memory_id, media_asset_id),
    CONSTRAINT ux_memory_images_order UNIQUE (memory_id, sort_order),
    CONSTRAINT ck_memory_images_sort_order CHECK (sort_order >= 0)
);

CREATE INDEX ix_memory_images_memory_section_order
    ON memory_images (memory_id, section_id, sort_order);

ALTER TABLE memories
    ADD CONSTRAINT fk_memories_cover_asset
        FOREIGN KEY (cover_asset_id) REFERENCES media_assets(id) ON DELETE RESTRICT;

ALTER TABLE memory_members
    ADD CONSTRAINT fk_memory_members_avatar_asset
        FOREIGN KEY (avatar_asset_id) REFERENCES media_assets(id) ON DELETE RESTRICT;

--rollback ALTER TABLE memory_members DROP CONSTRAINT fk_memory_members_avatar_asset;
--rollback ALTER TABLE memories DROP CONSTRAINT fk_memories_cover_asset;
--rollback DROP TABLE memory_images;
--rollback DROP TABLE media_assets;
