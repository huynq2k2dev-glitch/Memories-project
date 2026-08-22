--liquibase formatted sql

--changeset memories:002-auth-baseline
CREATE TABLE roles (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE permissions (
    id UUID PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id),
    permission_id UUID NOT NULL REFERENCES permissions(id),
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320),
    phone VARCHAR(32),
    password_hash VARCHAR(100),
    display_name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    email_verified_at TIMESTAMPTZ,
    phone_verified_at TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    locale VARCHAR(20) NOT NULL DEFAULT 'vi-VN',
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    failed_login_count INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_users_contact CHECK (email IS NOT NULL OR phone IS NOT NULL),
    CONSTRAINT ck_users_status CHECK (
        status IN ('PENDING_VERIFICATION', 'ACTIVE', 'LOCKED', 'DELETED')
    )
);

CREATE UNIQUE INDEX ux_users_email_active
    ON users (LOWER(email))
    WHERE deleted_at IS NULL AND email IS NOT NULL;
CREATE UNIQUE INDEX ux_users_phone_active
    ON users (phone)
    WHERE deleted_at IS NULL AND phone IS NOT NULL;
CREATE INDEX ix_users_status ON users (status);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id),
    role_id UUID NOT NULL REFERENCES roles(id),
    granted_by UUID REFERENCES users(id),
    granted_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE oauth_accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    provider VARCHAR(50) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    provider_email VARCHAR(320),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ux_oauth_provider_account UNIQUE (provider, provider_user_id),
    CONSTRAINT ux_oauth_user_provider UNIQUE (user_id, provider)
);

CREATE TABLE verification_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    token_hash CHAR(64) NOT NULL UNIQUE,
    type VARCHAR(32) NOT NULL,
    target VARCHAR(320) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_verification_token_type CHECK (
        type IN ('EMAIL_VERIFY', 'PASSWORD_RESET', 'PHONE_VERIFY')
    )
);
CREATE INDEX ix_verification_tokens_user_type
    ON verification_tokens (user_id, type);

INSERT INTO roles (id, code, name, description, is_system, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'USER', 'User', 'Default platform user', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000000002', 'ADMIN', 'Administrator', 'Platform administrator', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO permissions (id, code, name, description, created_at)
VALUES
    ('00000000-0000-0000-0000-000000000101', 'TEMPLATE_MANAGE', 'Manage templates', 'Create and publish platform templates', CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000000102', 'USER_MANAGE', 'Manage users', 'Manage platform user accounts', CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000000103', 'MEMORY_MODERATE', 'Moderate memories', 'Moderate published memories', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT '00000000-0000-0000-0000-000000000002', id, CURRENT_TIMESTAMP
FROM permissions;

--rollback DROP TABLE verification_tokens;
--rollback DROP TABLE oauth_accounts;
--rollback DROP TABLE user_roles;
--rollback DROP TABLE users;
--rollback DROP TABLE role_permissions;
--rollback DROP TABLE permissions;
--rollback DROP TABLE roles;

--changeset memories:002-token-hash-varchar
ALTER TABLE verification_tokens
    ALTER COLUMN token_hash TYPE VARCHAR(64);

--rollback ALTER TABLE verification_tokens ALTER COLUMN token_hash TYPE CHAR(64);
