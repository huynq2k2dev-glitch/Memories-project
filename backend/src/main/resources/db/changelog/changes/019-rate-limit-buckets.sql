--liquibase formatted sql

--changeset memories:019-rate-limit-buckets
CREATE TABLE rate_limit_buckets (
    scope VARCHAR(30) NOT NULL,
    subject_key VARCHAR(128) NOT NULL,
    window_started_at TIMESTAMPTZ NOT NULL,
    request_count INTEGER NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (scope, subject_key),
    CONSTRAINT ck_rate_limit_buckets_request_count CHECK (request_count >= 0)
);

CREATE INDEX ix_rate_limit_buckets_updated_at
    ON rate_limit_buckets (updated_at);

--rollback DROP TABLE rate_limit_buckets;
