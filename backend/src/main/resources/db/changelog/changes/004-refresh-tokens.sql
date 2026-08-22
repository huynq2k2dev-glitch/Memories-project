--liquibase formatted sql

--changeset memories:004-refresh-tokens
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    family_id UUID NOT NULL,
    parent_token_id UUID REFERENCES refresh_tokens(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    revocation_reason VARCHAR(40),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_refresh_tokens_family
    ON refresh_tokens (family_id);
CREATE INDEX ix_refresh_tokens_user_active
    ON refresh_tokens (user_id, expires_at)
    WHERE revoked_at IS NULL;
CREATE INDEX ix_refresh_tokens_parent
    ON refresh_tokens (parent_token_id)
    WHERE parent_token_id IS NOT NULL;

--rollback DROP TABLE refresh_tokens;
