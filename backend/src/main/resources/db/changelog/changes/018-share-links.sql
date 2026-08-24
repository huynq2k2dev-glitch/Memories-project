--liquibase formatted sql

--changeset memories:018-share-links
CREATE TABLE share_links (
    id UUID PRIMARY KEY,
    memory_id UUID NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    permission VARCHAR(20) NOT NULL,
    guest_id UUID REFERENCES memory_guests(id) ON DELETE RESTRICT,
    expires_at TIMESTAMPTZ,
    max_uses INTEGER,
    use_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT ck_share_links_permission CHECK (permission IN ('VIEW', 'RSVP')),
    CONSTRAINT ck_share_links_status CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED')),
    CONSTRAINT ck_share_links_max_uses CHECK (max_uses IS NULL OR max_uses > 0),
    CONSTRAINT ck_share_links_use_count CHECK (
        use_count >= 0 AND (max_uses IS NULL OR use_count <= max_uses)
    ),
    CONSTRAINT ck_share_links_guest_scope CHECK (
        (permission = 'VIEW' AND guest_id IS NULL)
        OR (permission = 'RSVP' AND guest_id IS NOT NULL)
    ),
    CONSTRAINT ck_share_links_revoked_at CHECK (
        (status = 'REVOKED' AND revoked_at IS NOT NULL)
        OR (status <> 'REVOKED' AND revoked_at IS NULL)
    )
);

CREATE INDEX ix_share_links_memory_status
    ON share_links (memory_id, status);

CREATE INDEX ix_share_links_expiry
    ON share_links (expires_at)
    WHERE status = 'ACTIVE' AND expires_at IS NOT NULL;

--rollback DROP TABLE share_links;
