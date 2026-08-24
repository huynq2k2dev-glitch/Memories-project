--liquibase formatted sql

--changeset memories:020-foundation-schema-alignment
ALTER TABLE users
    ADD COLUMN avatar_asset_id UUID,
    ADD CONSTRAINT fk_users_avatar_asset
        FOREIGN KEY (avatar_asset_id) REFERENCES media_assets(id) ON DELETE SET NULL;

ALTER TABLE templates
    ADD CONSTRAINT fk_templates_thumbnail_asset
        FOREIGN KEY (thumbnail_asset_id) REFERENCES media_assets(id) ON DELETE SET NULL;

ALTER TABLE role_permissions
    DROP CONSTRAINT role_permissions_role_id_fkey,
    DROP CONSTRAINT role_permissions_permission_id_fkey,
    ADD CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE;

ALTER TABLE user_roles
    DROP CONSTRAINT user_roles_user_id_fkey,
    DROP CONSTRAINT user_roles_role_id_fkey,
    DROP CONSTRAINT user_roles_granted_by_fkey,
    ADD CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_user_roles_granted_by_user
        FOREIGN KEY (granted_by) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE oauth_accounts
    DROP CONSTRAINT oauth_accounts_user_id_fkey,
    ADD CONSTRAINT fk_oauth_accounts_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE verification_tokens
    DROP CONSTRAINT verification_tokens_user_id_fkey,
    ADD CONSTRAINT fk_verification_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE refresh_tokens
    DROP CONSTRAINT refresh_tokens_user_id_fkey,
    DROP CONSTRAINT refresh_tokens_parent_token_id_fkey,
    ADD CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_refresh_tokens_parent
        FOREIGN KEY (parent_token_id) REFERENCES refresh_tokens(id) ON DELETE SET NULL;

ALTER TABLE memories
    DROP CONSTRAINT fk_memories_cover_asset,
    ADD CONSTRAINT fk_memories_cover_asset
        FOREIGN KEY (cover_asset_id) REFERENCES media_assets(id) ON DELETE SET NULL;

ALTER TABLE memory_members
    DROP CONSTRAINT fk_memory_members_avatar_asset,
    ADD CONSTRAINT fk_memory_members_avatar_asset
        FOREIGN KEY (avatar_asset_id) REFERENCES media_assets(id) ON DELETE SET NULL;

ALTER TABLE memory_collaborators
    DROP CONSTRAINT memory_collaborators_user_id_fkey,
    ADD CONSTRAINT fk_memory_collaborators_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE roles
    ADD CONSTRAINT ck_roles_code_upper CHECK (code = UPPER(code));

ALTER TABLE permissions
    ADD CONSTRAINT ck_permissions_code_upper CHECK (code = UPPER(code));

ALTER TABLE oauth_accounts
    ADD CONSTRAINT ck_oauth_accounts_provider CHECK (
        provider IN ('GOOGLE', 'FACEBOOK', 'APPLE')
    );

DELETE FROM verification_tokens
WHERE expires_at <= created_at;

ALTER TABLE verification_tokens
    ADD CONSTRAINT ck_verification_tokens_expiry CHECK (expires_at > created_at);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT ck_refresh_tokens_expiry CHECK (expires_at > created_at);

ALTER TABLE media_assets
    ADD CONSTRAINT ck_media_assets_ready_file_size CHECK (
        status <> 'READY' OR file_size > 0
    );

CREATE INDEX ix_role_permissions_permission
    ON role_permissions (permission_id);

CREATE INDEX ix_user_roles_role
    ON user_roles (role_id);

CREATE INDEX ix_oauth_accounts_user
    ON oauth_accounts (user_id);

ALTER TABLE auth_audit_events
    RENAME CONSTRAINT auth_audit_events_subject_user_id_fkey
        TO fk_auth_audit_events_subject_user;
ALTER TABLE auth_audit_events
    RENAME CONSTRAINT auth_audit_events_actor_user_id_fkey
        TO fk_auth_audit_events_actor_user;
ALTER TABLE template_versions
    RENAME CONSTRAINT template_versions_template_id_fkey
        TO fk_template_versions_template;
ALTER TABLE memories
    RENAME CONSTRAINT memories_owner_id_fkey TO fk_memories_owner;
ALTER TABLE memories
    RENAME CONSTRAINT memories_template_version_id_fkey
        TO fk_memories_template_version;
ALTER TABLE memories
    RENAME CONSTRAINT memories_created_by_fkey TO fk_memories_created_by_user;
ALTER TABLE memories
    RENAME CONSTRAINT memories_updated_by_fkey TO fk_memories_updated_by_user;
ALTER TABLE memory_members
    RENAME CONSTRAINT memory_members_memory_id_fkey TO fk_memory_members_memory;
ALTER TABLE memory_members
    RENAME CONSTRAINT memory_members_created_by_fkey
        TO fk_memory_members_created_by_user;
ALTER TABLE memory_members
    RENAME CONSTRAINT memory_members_updated_by_fkey
        TO fk_memory_members_updated_by_user;
ALTER TABLE memory_sections
    RENAME CONSTRAINT memory_sections_memory_id_fkey TO fk_memory_sections_memory;
ALTER TABLE memory_sections
    RENAME CONSTRAINT memory_sections_created_by_fkey
        TO fk_memory_sections_created_by_user;
ALTER TABLE memory_sections
    RENAME CONSTRAINT memory_sections_updated_by_fkey
        TO fk_memory_sections_updated_by_user;
ALTER TABLE memory_locations
    RENAME CONSTRAINT memory_locations_memory_id_fkey TO fk_memory_locations_memory;
ALTER TABLE memory_locations
    RENAME CONSTRAINT memory_locations_created_by_fkey
        TO fk_memory_locations_created_by_user;
ALTER TABLE memory_locations
    RENAME CONSTRAINT memory_locations_updated_by_fkey
        TO fk_memory_locations_updated_by_user;
ALTER TABLE memory_events
    RENAME CONSTRAINT memory_events_memory_id_fkey TO fk_memory_events_memory;
ALTER TABLE memory_events
    RENAME CONSTRAINT memory_events_location_id_fkey TO fk_memory_events_location;
ALTER TABLE memory_events
    RENAME CONSTRAINT memory_events_created_by_fkey
        TO fk_memory_events_created_by_user;
ALTER TABLE memory_events
    RENAME CONSTRAINT memory_events_updated_by_fkey
        TO fk_memory_events_updated_by_user;
ALTER TABLE media_assets
    RENAME CONSTRAINT media_assets_owner_id_fkey TO fk_media_assets_owner;
ALTER TABLE media_assets
    RENAME CONSTRAINT media_assets_parent_asset_id_fkey TO fk_media_assets_parent;
ALTER TABLE media_assets
    RENAME CONSTRAINT media_assets_created_by_fkey
        TO fk_media_assets_created_by_user;
ALTER TABLE media_assets
    RENAME CONSTRAINT media_assets_updated_by_fkey
        TO fk_media_assets_updated_by_user;
ALTER TABLE memory_images
    RENAME CONSTRAINT memory_images_memory_id_fkey TO fk_memory_images_memory;
ALTER TABLE memory_images
    RENAME CONSTRAINT memory_images_media_asset_id_fkey TO fk_memory_images_media_asset;
ALTER TABLE memory_images
    RENAME CONSTRAINT memory_images_section_id_fkey TO fk_memory_images_section;
ALTER TABLE memory_images
    RENAME CONSTRAINT memory_images_created_by_fkey
        TO fk_memory_images_created_by_user;
ALTER TABLE memory_images
    RENAME CONSTRAINT memory_images_updated_by_fkey
        TO fk_memory_images_updated_by_user;
ALTER TABLE audit_logs
    RENAME CONSTRAINT audit_logs_actor_user_id_fkey TO fk_audit_logs_actor_user;
ALTER TABLE memory_access_grants
    RENAME CONSTRAINT memory_access_grants_memory_id_fkey
        TO fk_memory_access_grants_memory;
ALTER TABLE memory_collaborators
    RENAME CONSTRAINT memory_collaborators_memory_id_fkey
        TO fk_memory_collaborators_memory;
ALTER TABLE memory_collaborators
    RENAME CONSTRAINT memory_collaborators_invited_by_fkey
        TO fk_memory_collaborators_invited_by_user;
ALTER TABLE memory_guests
    RENAME CONSTRAINT memory_guests_memory_id_fkey TO fk_memory_guests_memory;
ALTER TABLE memory_guests
    RENAME CONSTRAINT memory_guests_created_by_fkey
        TO fk_memory_guests_created_by_user;
ALTER TABLE memory_guests
    RENAME CONSTRAINT memory_guests_updated_by_fkey
        TO fk_memory_guests_updated_by_user;
ALTER TABLE guest_event_responses
    RENAME CONSTRAINT guest_event_responses_guest_id_fkey
        TO fk_guest_event_responses_guest;
ALTER TABLE guest_event_responses
    RENAME CONSTRAINT guest_event_responses_event_id_fkey
        TO fk_guest_event_responses_event;
ALTER TABLE guest_messages
    RENAME CONSTRAINT guest_messages_memory_id_fkey TO fk_guest_messages_memory;
ALTER TABLE guest_messages
    RENAME CONSTRAINT guest_messages_guest_id_fkey TO fk_guest_messages_guest;
ALTER TABLE guest_messages
    RENAME CONSTRAINT guest_messages_moderated_by_fkey
        TO fk_guest_messages_moderated_by_user;
ALTER TABLE share_links
    RENAME CONSTRAINT share_links_memory_id_fkey TO fk_share_links_memory;
ALTER TABLE share_links
    RENAME CONSTRAINT share_links_guest_id_fkey TO fk_share_links_guest;
ALTER TABLE share_links
    RENAME CONSTRAINT share_links_created_by_fkey TO fk_share_links_created_by_user;

UPDATE roles
SET created_at = TIMESTAMPTZ '2026-01-01 00:00:00+00',
    updated_at = TIMESTAMPTZ '2026-01-01 00:00:00+00'
WHERE id IN (
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000002'
);

UPDATE permissions
SET created_at = TIMESTAMPTZ '2026-01-01 00:00:00+00'
WHERE id IN (
    '00000000-0000-0000-0000-000000000101',
    '00000000-0000-0000-0000-000000000102',
    '00000000-0000-0000-0000-000000000103'
);

UPDATE role_permissions
SET created_at = TIMESTAMPTZ '2026-01-01 00:00:00+00'
WHERE role_id = '00000000-0000-0000-0000-000000000002'
  AND permission_id IN (
      '00000000-0000-0000-0000-000000000101',
      '00000000-0000-0000-0000-000000000102',
      '00000000-0000-0000-0000-000000000103'
  );

--rollback UPDATE role_permissions SET created_at = CURRENT_TIMESTAMP WHERE role_id = '00000000-0000-0000-0000-000000000002' AND permission_id IN ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000102', '00000000-0000-0000-0000-000000000103');
--rollback UPDATE permissions SET created_at = CURRENT_TIMESTAMP WHERE id IN ('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000102', '00000000-0000-0000-0000-000000000103');
--rollback UPDATE roles SET created_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id IN ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002');
--rollback ALTER TABLE share_links RENAME CONSTRAINT fk_share_links_created_by_user TO share_links_created_by_fkey;
--rollback ALTER TABLE share_links RENAME CONSTRAINT fk_share_links_guest TO share_links_guest_id_fkey;
--rollback ALTER TABLE share_links RENAME CONSTRAINT fk_share_links_memory TO share_links_memory_id_fkey;
--rollback ALTER TABLE guest_messages RENAME CONSTRAINT fk_guest_messages_moderated_by_user TO guest_messages_moderated_by_fkey;
--rollback ALTER TABLE guest_messages RENAME CONSTRAINT fk_guest_messages_guest TO guest_messages_guest_id_fkey;
--rollback ALTER TABLE guest_messages RENAME CONSTRAINT fk_guest_messages_memory TO guest_messages_memory_id_fkey;
--rollback ALTER TABLE guest_event_responses RENAME CONSTRAINT fk_guest_event_responses_event TO guest_event_responses_event_id_fkey;
--rollback ALTER TABLE guest_event_responses RENAME CONSTRAINT fk_guest_event_responses_guest TO guest_event_responses_guest_id_fkey;
--rollback ALTER TABLE memory_guests RENAME CONSTRAINT fk_memory_guests_updated_by_user TO memory_guests_updated_by_fkey;
--rollback ALTER TABLE memory_guests RENAME CONSTRAINT fk_memory_guests_created_by_user TO memory_guests_created_by_fkey;
--rollback ALTER TABLE memory_guests RENAME CONSTRAINT fk_memory_guests_memory TO memory_guests_memory_id_fkey;
--rollback ALTER TABLE memory_collaborators RENAME CONSTRAINT fk_memory_collaborators_invited_by_user TO memory_collaborators_invited_by_fkey;
--rollback ALTER TABLE memory_collaborators RENAME CONSTRAINT fk_memory_collaborators_memory TO memory_collaborators_memory_id_fkey;
--rollback ALTER TABLE memory_access_grants RENAME CONSTRAINT fk_memory_access_grants_memory TO memory_access_grants_memory_id_fkey;
--rollback ALTER TABLE audit_logs RENAME CONSTRAINT fk_audit_logs_actor_user TO audit_logs_actor_user_id_fkey;
--rollback ALTER TABLE memory_images RENAME CONSTRAINT fk_memory_images_updated_by_user TO memory_images_updated_by_fkey;
--rollback ALTER TABLE memory_images RENAME CONSTRAINT fk_memory_images_created_by_user TO memory_images_created_by_fkey;
--rollback ALTER TABLE memory_images RENAME CONSTRAINT fk_memory_images_section TO memory_images_section_id_fkey;
--rollback ALTER TABLE memory_images RENAME CONSTRAINT fk_memory_images_media_asset TO memory_images_media_asset_id_fkey;
--rollback ALTER TABLE memory_images RENAME CONSTRAINT fk_memory_images_memory TO memory_images_memory_id_fkey;
--rollback ALTER TABLE media_assets RENAME CONSTRAINT fk_media_assets_updated_by_user TO media_assets_updated_by_fkey;
--rollback ALTER TABLE media_assets RENAME CONSTRAINT fk_media_assets_created_by_user TO media_assets_created_by_fkey;
--rollback ALTER TABLE media_assets RENAME CONSTRAINT fk_media_assets_parent TO media_assets_parent_asset_id_fkey;
--rollback ALTER TABLE media_assets RENAME CONSTRAINT fk_media_assets_owner TO media_assets_owner_id_fkey;
--rollback ALTER TABLE memory_events RENAME CONSTRAINT fk_memory_events_updated_by_user TO memory_events_updated_by_fkey;
--rollback ALTER TABLE memory_events RENAME CONSTRAINT fk_memory_events_created_by_user TO memory_events_created_by_fkey;
--rollback ALTER TABLE memory_events RENAME CONSTRAINT fk_memory_events_location TO memory_events_location_id_fkey;
--rollback ALTER TABLE memory_events RENAME CONSTRAINT fk_memory_events_memory TO memory_events_memory_id_fkey;
--rollback ALTER TABLE memory_locations RENAME CONSTRAINT fk_memory_locations_updated_by_user TO memory_locations_updated_by_fkey;
--rollback ALTER TABLE memory_locations RENAME CONSTRAINT fk_memory_locations_created_by_user TO memory_locations_created_by_fkey;
--rollback ALTER TABLE memory_locations RENAME CONSTRAINT fk_memory_locations_memory TO memory_locations_memory_id_fkey;
--rollback ALTER TABLE memory_sections RENAME CONSTRAINT fk_memory_sections_updated_by_user TO memory_sections_updated_by_fkey;
--rollback ALTER TABLE memory_sections RENAME CONSTRAINT fk_memory_sections_created_by_user TO memory_sections_created_by_fkey;
--rollback ALTER TABLE memory_sections RENAME CONSTRAINT fk_memory_sections_memory TO memory_sections_memory_id_fkey;
--rollback ALTER TABLE memory_members RENAME CONSTRAINT fk_memory_members_updated_by_user TO memory_members_updated_by_fkey;
--rollback ALTER TABLE memory_members RENAME CONSTRAINT fk_memory_members_created_by_user TO memory_members_created_by_fkey;
--rollback ALTER TABLE memory_members RENAME CONSTRAINT fk_memory_members_memory TO memory_members_memory_id_fkey;
--rollback ALTER TABLE memories RENAME CONSTRAINT fk_memories_updated_by_user TO memories_updated_by_fkey;
--rollback ALTER TABLE memories RENAME CONSTRAINT fk_memories_created_by_user TO memories_created_by_fkey;
--rollback ALTER TABLE memories RENAME CONSTRAINT fk_memories_template_version TO memories_template_version_id_fkey;
--rollback ALTER TABLE memories RENAME CONSTRAINT fk_memories_owner TO memories_owner_id_fkey;
--rollback ALTER TABLE template_versions RENAME CONSTRAINT fk_template_versions_template TO template_versions_template_id_fkey;
--rollback ALTER TABLE auth_audit_events RENAME CONSTRAINT fk_auth_audit_events_actor_user TO auth_audit_events_actor_user_id_fkey;
--rollback ALTER TABLE auth_audit_events RENAME CONSTRAINT fk_auth_audit_events_subject_user TO auth_audit_events_subject_user_id_fkey;
--rollback DROP INDEX ix_oauth_accounts_user;
--rollback DROP INDEX ix_user_roles_role;
--rollback DROP INDEX ix_role_permissions_permission;
--rollback ALTER TABLE media_assets DROP CONSTRAINT ck_media_assets_ready_file_size;
--rollback ALTER TABLE refresh_tokens DROP CONSTRAINT ck_refresh_tokens_expiry;
--rollback ALTER TABLE verification_tokens DROP CONSTRAINT ck_verification_tokens_expiry;
--rollback ALTER TABLE oauth_accounts DROP CONSTRAINT ck_oauth_accounts_provider;
--rollback SELECT 1; -- Invalid verification tokens removed during upgrade cannot be restored.
--rollback ALTER TABLE permissions DROP CONSTRAINT ck_permissions_code_upper;
--rollback ALTER TABLE roles DROP CONSTRAINT ck_roles_code_upper;
--rollback ALTER TABLE memory_collaborators DROP CONSTRAINT fk_memory_collaborators_user, ADD CONSTRAINT memory_collaborators_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT;
--rollback ALTER TABLE memory_members DROP CONSTRAINT fk_memory_members_avatar_asset, ADD CONSTRAINT fk_memory_members_avatar_asset FOREIGN KEY (avatar_asset_id) REFERENCES media_assets(id) ON DELETE RESTRICT;
--rollback ALTER TABLE memories DROP CONSTRAINT fk_memories_cover_asset, ADD CONSTRAINT fk_memories_cover_asset FOREIGN KEY (cover_asset_id) REFERENCES media_assets(id) ON DELETE RESTRICT;
--rollback ALTER TABLE refresh_tokens DROP CONSTRAINT fk_refresh_tokens_parent, DROP CONSTRAINT fk_refresh_tokens_user, ADD CONSTRAINT refresh_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id), ADD CONSTRAINT refresh_tokens_parent_token_id_fkey FOREIGN KEY (parent_token_id) REFERENCES refresh_tokens(id);
--rollback ALTER TABLE verification_tokens DROP CONSTRAINT fk_verification_tokens_user, ADD CONSTRAINT verification_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id);
--rollback ALTER TABLE oauth_accounts DROP CONSTRAINT fk_oauth_accounts_user, ADD CONSTRAINT oauth_accounts_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id);
--rollback ALTER TABLE user_roles DROP CONSTRAINT fk_user_roles_granted_by_user, DROP CONSTRAINT fk_user_roles_role, DROP CONSTRAINT fk_user_roles_user, ADD CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id), ADD CONSTRAINT user_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES roles(id), ADD CONSTRAINT user_roles_granted_by_fkey FOREIGN KEY (granted_by) REFERENCES users(id);
--rollback ALTER TABLE role_permissions DROP CONSTRAINT fk_role_permissions_permission, DROP CONSTRAINT fk_role_permissions_role, ADD CONSTRAINT role_permissions_role_id_fkey FOREIGN KEY (role_id) REFERENCES roles(id), ADD CONSTRAINT role_permissions_permission_id_fkey FOREIGN KEY (permission_id) REFERENCES permissions(id);
--rollback ALTER TABLE templates DROP CONSTRAINT fk_templates_thumbnail_asset;
--rollback ALTER TABLE users DROP CONSTRAINT fk_users_avatar_asset, DROP COLUMN avatar_asset_id;
