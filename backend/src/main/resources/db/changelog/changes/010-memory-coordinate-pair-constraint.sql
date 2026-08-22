--liquibase formatted sql

--changeset memories:010-memory-coordinate-pair-constraint
ALTER TABLE memory_locations
    DROP CONSTRAINT ck_memory_locations_coordinates;

ALTER TABLE memory_locations
    ADD CONSTRAINT ck_memory_locations_coordinates CHECK (
        (latitude IS NULL AND longitude IS NULL)
        OR (
            latitude IS NOT NULL
            AND longitude IS NOT NULL
            AND latitude BETWEEN -90 AND 90
            AND longitude BETWEEN -180 AND 180
        )
    );

--rollback ALTER TABLE memory_locations DROP CONSTRAINT ck_memory_locations_coordinates;
--rollback ALTER TABLE memory_locations ADD CONSTRAINT ck_memory_locations_coordinates CHECK ((latitude IS NULL AND longitude IS NULL) OR (latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180));
