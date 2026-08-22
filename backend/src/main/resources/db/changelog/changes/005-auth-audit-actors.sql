--liquibase formatted sql

--changeset memories:005-auth-audit-actors
ALTER TABLE auth_audit_events
    ADD COLUMN actor_user_id UUID REFERENCES users(id),
    ADD COLUMN target_user_id UUID;

CREATE INDEX ix_auth_audit_events_actor_occurred
    ON auth_audit_events (actor_user_id, occurred_at DESC)
    WHERE actor_user_id IS NOT NULL;

--rollback DROP INDEX ix_auth_audit_events_actor_occurred;
--rollback ALTER TABLE auth_audit_events DROP COLUMN target_user_id, DROP COLUMN actor_user_id;
