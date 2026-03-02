-- V105: Parent letter file attachments
CREATE TABLE parent_letter_attachments (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    letter_id         UUID NOT NULL REFERENCES parent_letters(id) ON DELETE CASCADE,
    original_filename VARCHAR(500) NOT NULL,
    storage_path      VARCHAR(500) NOT NULL,
    file_size         BIGINT NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    uploaded_by       UUID REFERENCES users(id) ON DELETE SET NULL,
    sort_order        INTEGER NOT NULL DEFAULT 0,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_pla_letter_id ON parent_letter_attachments(letter_id);
