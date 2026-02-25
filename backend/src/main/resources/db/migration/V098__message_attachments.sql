-- Message attachments: PDFs and file links (collaborative documents)
CREATE TABLE message_attachments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id      UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    attachment_type  VARCHAR(20) NOT NULL,  -- FILE or FILE_LINK
    uploaded_by     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- For FILE type: uploaded file stored in MinIO
    original_filename VARCHAR(500),
    storage_path     VARCHAR(1000),
    file_size        BIGINT,
    content_type     VARCHAR(200),

    -- For FILE_LINK type: reference to a room file
    linked_file_id   UUID REFERENCES room_files(id) ON DELETE SET NULL,
    linked_file_name VARCHAR(500),
    linked_room_id   UUID REFERENCES rooms(id) ON DELETE SET NULL,

    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_message_attachments_message_id ON message_attachments(message_id);
