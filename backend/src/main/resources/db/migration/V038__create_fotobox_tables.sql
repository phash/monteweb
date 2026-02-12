-- V038: Fotobox module tables

-- Fotobox settings per room
CREATE TABLE fotobox_room_settings (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id               UUID NOT NULL UNIQUE REFERENCES rooms(id) ON DELETE CASCADE,
    enabled               BOOLEAN NOT NULL DEFAULT FALSE,
    default_permission    VARCHAR(20) NOT NULL DEFAULT 'VIEW_ONLY',
    max_images_per_thread INT DEFAULT NULL,
    max_file_size_mb      INT NOT NULL DEFAULT 10,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Photo threads
CREATE TABLE fotobox_threads (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id         UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    title           VARCHAR(300) NOT NULL,
    description     TEXT,
    cover_image_id  UUID,
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fotobox_threads_room ON fotobox_threads(room_id);

-- Photos
CREATE TABLE fotobox_images (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    thread_id         UUID NOT NULL REFERENCES fotobox_threads(id) ON DELETE CASCADE,
    uploaded_by       UUID NOT NULL REFERENCES users(id),
    original_filename VARCHAR(255) NOT NULL,
    storage_path      VARCHAR(500) NOT NULL,
    thumbnail_path    VARCHAR(500),
    file_size         BIGINT NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    width             INT,
    height            INT,
    caption           VARCHAR(500),
    sort_order        INT NOT NULL DEFAULT 0,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fotobox_images_thread ON fotobox_images(thread_id);

-- Cover image foreign key (deferred because fotobox_images must exist first)
ALTER TABLE fotobox_threads
    ADD CONSTRAINT fk_fotobox_cover_image
    FOREIGN KEY (cover_image_id) REFERENCES fotobox_images(id) ON DELETE SET NULL;

-- Add fotobox to default modules in tenant_config
UPDATE tenant_config SET modules = modules || '{"fotobox": true}'::jsonb;
