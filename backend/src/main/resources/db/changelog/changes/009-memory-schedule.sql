--liquibase formatted sql

--changeset memories:009-memory-schedule
CREATE TABLE memory_locations (
    id UUID PRIMARY KEY,
    memory_id UUID NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    address VARCHAR(500),
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    map_url VARCHAR(2048),
    note VARCHAR(1000),
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_memory_locations_coordinates CHECK (
        (latitude IS NULL AND longitude IS NULL)
        OR (
            latitude BETWEEN -90 AND 90
            AND longitude BETWEEN -180 AND 180
        )
    ),
    CONSTRAINT ck_memory_locations_map_url CHECK (
        map_url IS NULL OR map_url LIKE 'https://%'
    ),
    CONSTRAINT ck_memory_locations_sort_order CHECK (sort_order >= 0)
);

CREATE INDEX ix_memory_locations_memory_order
    ON memory_locations (memory_id, sort_order);

CREATE TABLE memory_events (
    id UUID PRIMARY KEY,
    memory_id UUID NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    location_id UUID REFERENCES memory_locations(id) ON DELETE SET NULL,
    event_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ,
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    sort_order INTEGER NOT NULL,
    rsvp_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ux_memory_events_order UNIQUE (memory_id, sort_order),
    CONSTRAINT ck_memory_events_type CHECK (
        event_type ~ '^[A-Z][A-Z0-9_]{0,49}$'
    ),
    CONSTRAINT ck_memory_events_time CHECK (
        end_at IS NULL OR end_at >= start_at
    ),
    CONSTRAINT ck_memory_events_sort_order CHECK (sort_order >= 0)
);

CREATE INDEX ix_memory_events_memory_start
    ON memory_events (memory_id, start_at);

--rollback DROP TABLE memory_events;
--rollback DROP TABLE memory_locations;
