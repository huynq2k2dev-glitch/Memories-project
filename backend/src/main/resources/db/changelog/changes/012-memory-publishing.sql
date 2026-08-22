--liquibase formatted sql

--changeset memories:012-template-cover-contract
ALTER TABLE template_versions
    ADD COLUMN cover_required BOOLEAN NOT NULL DEFAULT FALSE;

--rollback ALTER TABLE template_versions DROP COLUMN cover_required;

--changeset memories:012-memory-published-timestamp
ALTER TABLE memories
    ADD CONSTRAINT ck_memories_published_at CHECK (
        status <> 'PUBLISHED' OR published_at IS NOT NULL
    );

--rollback ALTER TABLE memories DROP CONSTRAINT ck_memories_published_at;

--changeset memories:012-audit-logs
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    result VARCHAR(20) NOT NULL,
    correlation_id VARCHAR(100),
    ip_hash VARCHAR(128),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_audit_logs_result CHECK (result IN ('SUCCESS', 'FAILURE', 'DENIED')),
    CONSTRAINT ck_audit_logs_metadata CHECK (jsonb_typeof(metadata) = 'object')
);

CREATE INDEX ix_audit_logs_actor_created
    ON audit_logs (actor_user_id, created_at DESC);
CREATE INDEX ix_audit_logs_entity_created
    ON audit_logs (entity_type, entity_id, created_at DESC);
CREATE INDEX ix_audit_logs_action_created
    ON audit_logs (action, created_at DESC);

--rollback DROP TABLE audit_logs;

--changeset memories:012-audit-logs-append-only-function splitStatements:false
CREATE FUNCTION reject_audit_log_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs is append-only';
END;
$$ LANGUAGE plpgsql;

--rollback DROP FUNCTION reject_audit_log_mutation();

--changeset memories:012-audit-logs-append-only-trigger
CREATE TRIGGER trg_audit_logs_append_only
    BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW
    EXECUTE FUNCTION reject_audit_log_mutation();

--rollback DROP TRIGGER trg_audit_logs_append_only ON audit_logs;
