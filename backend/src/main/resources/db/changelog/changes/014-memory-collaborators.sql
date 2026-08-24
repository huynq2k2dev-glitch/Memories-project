--liquibase formatted sql

--changeset memories:014-memory-collaborators
CREATE TABLE memory_collaborators (
    id UUID PRIMARY KEY,
    memory_id UUID NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    permission VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    invited_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT ux_memory_collaborators_memory_user UNIQUE (memory_id, user_id),
    CONSTRAINT ck_memory_collaborators_permission CHECK (
        permission IN ('VIEW', 'EDIT', 'ADMIN')
    ),
    CONSTRAINT ck_memory_collaborators_status CHECK (
        status IN ('ACTIVE', 'REVOKED')
    ),
    CONSTRAINT ck_memory_collaborators_revoked_at CHECK (
        (status = 'ACTIVE' AND revoked_at IS NULL)
        OR (status = 'REVOKED' AND revoked_at IS NOT NULL)
    )
);

CREATE INDEX ix_memory_collaborators_user_status
    ON memory_collaborators (user_id, status);

CREATE INDEX ix_memory_collaborators_memory_status
    ON memory_collaborators (memory_id, status);

--rollback DROP TABLE memory_collaborators;
