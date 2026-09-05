--liquibase formatted sql

--changeset memories:022-html-book-templates
ALTER TABLE template_versions ADD COLUMN book_config JSONB;
ALTER TABLE template_versions ADD COLUMN css_content TEXT;
ALTER TABLE template_versions ADD CONSTRAINT ck_template_book_config
    CHECK (book_config IS NULL OR jsonb_typeof(book_config) = 'object');

CREATE TABLE template_pages (
    template_version_id UUID NOT NULL REFERENCES template_versions(id) ON DELETE CASCADE,
    page_order INTEGER NOT NULL CHECK (page_order >= 0),
    page_key VARCHAR(60) NOT NULL,
    name VARCHAR(120) NOT NULL,
    page_type VARCHAR(30) NOT NULL,
    html_content TEXT NOT NULL,
    PRIMARY KEY (template_version_id, page_order),
    UNIQUE (template_version_id, page_key) DEFERRABLE INITIALLY DEFERRED
);

--rollback DROP TABLE template_pages;
--rollback ALTER TABLE template_versions DROP CONSTRAINT ck_template_book_config;
--rollback ALTER TABLE template_versions DROP COLUMN css_content;
--rollback ALTER TABLE template_versions DROP COLUMN book_config;
