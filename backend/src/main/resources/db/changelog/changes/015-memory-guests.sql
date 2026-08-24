--liquibase formatted sql

--changeset memories:015-memory-guests
CREATE TABLE memory_guests (
    id UUID PRIMARY KEY,
    memory_id UUID NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    guest_group VARCHAR(100),
    max_party_size INTEGER NOT NULL DEFAULT 1,
    access_token_hash VARCHAR(128) UNIQUE,
    note VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_memory_guests_party_size CHECK (max_party_size BETWEEN 1 AND 50),
    CONSTRAINT ck_memory_guests_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_memory_guests_phone CHECK (
        phone IS NULL OR phone ~ '^\+[1-9][0-9]{1,14}$'
    )
);

CREATE INDEX ix_memory_guests_memory_status
    ON memory_guests (memory_id, status);

CREATE INDEX ix_memory_guests_memory_group
    ON memory_guests (memory_id, guest_group);

--rollback DROP TABLE memory_guests;
