-- V055: Add image support and reply-to for chat messages

-- Allow image-only messages (no text content required)
ALTER TABLE messages ALTER COLUMN content DROP NOT NULL;

-- Reply-to support
ALTER TABLE messages ADD COLUMN reply_to_id UUID REFERENCES messages(id) ON DELETE SET NULL;

-- Message images table
CREATE TABLE message_images (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id      UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    uploaded_by     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    original_filename VARCHAR(255) NOT NULL,
    storage_path    VARCHAR(500) NOT NULL,
    thumbnail_path  VARCHAR(500),
    file_size       BIGINT NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_message_images_message_id ON message_images(message_id);
CREATE INDEX idx_message_images_created_at ON message_images(created_at);
