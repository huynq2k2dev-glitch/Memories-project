--liquibase formatted sql

--changeset memories:013-memory-password-access
CREATE TABLE memory_access_grants (
    id UUID PRIMARY KEY,
    memory_id UUID NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_memory_access_grants_memory_expiry
    ON memory_access_grants (memory_id, expires_at);

CREATE INDEX ix_memory_access_grants_expiry
    ON memory_access_grants (expires_at);

--rollback DROP TABLE memory_access_grants;
