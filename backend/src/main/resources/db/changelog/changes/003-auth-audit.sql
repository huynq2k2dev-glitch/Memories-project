--liquibase formatted sql

--changeset memories:003-auth-audit
CREATE TABLE auth_audit_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    reason_code VARCHAR(50),
    subject_user_id UUID REFERENCES users(id),
    correlation_id VARCHAR(100),
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_auth_audit_events_subject_occurred
    ON auth_audit_events (subject_user_id, occurred_at DESC);
CREATE INDEX ix_auth_audit_events_type_occurred
    ON auth_audit_events (event_type, occurred_at DESC);

--rollback DROP TABLE auth_audit_events;

--changeset memories:003-auth-audit-append-only-function splitStatements:false
CREATE FUNCTION reject_auth_audit_event_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'auth_audit_events is append-only';
END;
$$ LANGUAGE plpgsql;

--rollback DROP FUNCTION reject_auth_audit_event_mutation();

--changeset memories:003-auth-audit-append-only-trigger
CREATE TRIGGER trg_auth_audit_events_append_only
    BEFORE UPDATE OR DELETE ON auth_audit_events
    FOR EACH ROW
    EXECUTE FUNCTION reject_auth_audit_event_mutation();

--rollback DROP TRIGGER trg_auth_audit_events_append_only ON auth_audit_events;
