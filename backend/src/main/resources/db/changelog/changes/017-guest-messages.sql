--liquibase formatted sql

--changeset memories:017-guest-messages
UPDATE memories
SET settings = jsonb_set(
        settings,
        '{messageModerationEnabled}',
        'true'::jsonb,
        true
    )
WHERE settings -> 'messageModerationEnabled' IS NULL;

CREATE TABLE guest_messages (
    id UUID PRIMARY KEY,
    memory_id UUID NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    guest_id UUID REFERENCES memory_guests(id) ON DELETE SET NULL,
    guest_name VARCHAR(200) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    ip_hash VARCHAR(128),
    moderated_by UUID REFERENCES users(id) ON DELETE SET NULL,
    moderated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_guest_messages_guest_name CHECK (
        char_length(btrim(guest_name)) BETWEEN 1 AND 200
    ),
    CONSTRAINT ck_guest_messages_content CHECK (
        char_length(btrim(content)) BETWEEN 1 AND 2000
    ),
    CONSTRAINT ck_guest_messages_status CHECK (
        status IN ('PENDING', 'APPROVED', 'REJECTED', 'HIDDEN')
    )
);

CREATE INDEX ix_guest_messages_memory_status_created
    ON guest_messages (memory_id, status, created_at);

CREATE INDEX ix_guest_messages_rate_limit
    ON guest_messages (memory_id, ip_hash, created_at)
    WHERE ip_hash IS NOT NULL;

--rollback DROP TABLE guest_messages; UPDATE memories SET settings = settings - 'messageModerationEnabled';
