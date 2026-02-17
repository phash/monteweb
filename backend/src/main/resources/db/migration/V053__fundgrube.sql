-- V053: Fundgrube (Lost & Found) module

CREATE TABLE fundgrube_items (
    id          UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    title       VARCHAR(300) NOT NULL,
    description TEXT,
    section_id  UUID,  -- optional, for section filter
    created_by  UUID NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    claimed_by  UUID,
    claimed_at  TIMESTAMP WITH TIME ZONE,
    expires_at  TIMESTAMP WITH TIME ZONE  -- claimed_at + 1 day, set on claim
);

CREATE INDEX idx_fundgrube_items_section ON fundgrube_items (section_id);
CREATE INDEX idx_fundgrube_items_expires ON fundgrube_items (expires_at);
CREATE INDEX idx_fundgrube_items_created ON fundgrube_items (created_at DESC);

CREATE TABLE fundgrube_images (
    id                UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    item_id           UUID NOT NULL REFERENCES fundgrube_items (id) ON DELETE CASCADE,
    storage_path      VARCHAR(500) NOT NULL,
    thumbnail_path    VARCHAR(500),
    original_filename VARCHAR(255) NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    file_size         BIGINT NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_fundgrube_images_item ON fundgrube_images (item_id);
