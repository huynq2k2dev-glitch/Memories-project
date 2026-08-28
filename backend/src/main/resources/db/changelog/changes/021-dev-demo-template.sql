--liquibase formatted sql

--changeset memories:021-dev-demo-template context:dev
INSERT INTO templates (
    id,
    code,
    name,
    memory_type,
    description,
    status,
    created_at,
    updated_at
)
VALUES (
    '10000000-0000-0000-0000-000000000001',
    'PERSONAL_DEMO',
    'Nhật ký kỷ niệm',
    'PERSONAL',
    'Template demo cho luồng tạo memory trên môi trường local.',
    'ACTIVE',
    TIMESTAMPTZ '2026-08-25 00:00:00+00',
    TIMESTAMPTZ '2026-08-25 00:00:00+00'
)
ON CONFLICT DO NOTHING;

INSERT INTO template_versions (
    id,
    template_id,
    version_no,
    component_key,
    renderer_version,
    cover_required,
    config_schema,
    default_config,
    required_sections,
    section_contracts,
    status,
    published_at,
    created_at,
    updated_at
)
SELECT
    '10000000-0000-0000-0000-000000000101',
    template.id,
    1,
    'memories-basic-v1',
    '1',
    FALSE,
    '{
      "$schema": "https://json-schema.org/draft/2020-12/schema",
      "type": "object",
      "properties": {
        "accentColor": { "type": "string" },
        "subtitle": { "type": "string" }
      },
      "additionalProperties": false
    }'::jsonb,
    '{
      "accentColor": "#9b4d54",
      "subtitle": "Nơi những khoảnh khắc quan trọng được lưu giữ."
    }'::jsonb,
    '[]'::jsonb,
    '{
      "STORY": {
        "configSchema": {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "type": "object",
          "additionalProperties": true
        }
      }
    }'::jsonb,
    'PUBLISHED',
    TIMESTAMPTZ '2026-08-25 00:00:00+00',
    TIMESTAMPTZ '2026-08-25 00:00:00+00',
    TIMESTAMPTZ '2026-08-25 00:00:00+00'
FROM templates template
WHERE template.code = 'PERSONAL_DEMO'
ON CONFLICT DO NOTHING;

--rollback DELETE FROM template_versions WHERE id = '10000000-0000-0000-0000-000000000101';
--rollback DELETE FROM templates WHERE id = '10000000-0000-0000-0000-000000000001';
