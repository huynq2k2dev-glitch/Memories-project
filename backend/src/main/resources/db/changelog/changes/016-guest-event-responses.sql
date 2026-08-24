--liquibase formatted sql

--changeset memories:016-guest-event-responses
CREATE TABLE guest_event_responses (
    id UUID PRIMARY KEY,
    guest_id UUID NOT NULL REFERENCES memory_guests(id) ON DELETE CASCADE,
    event_id UUID NOT NULL REFERENCES memory_events(id) ON DELETE CASCADE,
    attendance_status VARCHAR(20) NOT NULL,
    party_size INTEGER NOT NULL DEFAULT 1,
    dietary_note VARCHAR(500),
    message VARCHAR(1000),
    responded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ux_guest_event_responses_guest_event UNIQUE (guest_id, event_id),
    CONSTRAINT ck_guest_event_responses_status CHECK (
        attendance_status IN ('PENDING', 'ATTENDING', 'DECLINED', 'MAYBE')
    ),
    CONSTRAINT ck_guest_event_responses_party_size CHECK (
        party_size BETWEEN 0 AND 50
    ),
    CONSTRAINT ck_guest_event_responses_declined_party CHECK (
        (attendance_status = 'DECLINED' AND party_size = 0)
        OR (attendance_status <> 'DECLINED' AND party_size BETWEEN 1 AND 50)
    ),
    CONSTRAINT ck_guest_event_responses_responded_at CHECK (
        (attendance_status = 'PENDING' AND responded_at IS NULL)
        OR (attendance_status <> 'PENDING' AND responded_at IS NOT NULL)
    )
);

CREATE INDEX ix_guest_event_responses_event_status
    ON guest_event_responses (event_id, attendance_status);

--rollback DROP TABLE guest_event_responses;
