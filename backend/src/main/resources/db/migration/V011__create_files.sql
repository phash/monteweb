CREATE TABLE room_folders (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id     UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    parent_id   UUID REFERENCES room_folders(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    created_by  UUID NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE room_files (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id         UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    folder_id       UUID REFERENCES room_folders(id) ON DELETE SET NULL,
    original_name   VARCHAR(500) NOT NULL,
    stored_name     VARCHAR(500) NOT NULL,
    content_type    VARCHAR(200),
    file_size       BIGINT NOT NULL,
    storage_path    VARCHAR(1000) NOT NULL,
    uploaded_by     UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_room_folders_room ON room_folders(room_id);
CREATE INDEX idx_room_files_room ON room_files(room_id);
CREATE INDEX idx_room_files_folder ON room_files(folder_id);
