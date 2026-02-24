-- Content Bookmarks / Lesezeichen
CREATE TABLE bookmarks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content_type VARCHAR(20) NOT NULL,
    content_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_bookmark UNIQUE (user_id, content_type, content_id)
);

CREATE INDEX idx_bookmarks_user ON bookmarks(user_id);
CREATE INDEX idx_bookmarks_user_type ON bookmarks(user_id, content_type);
CREATE INDEX idx_bookmarks_content ON bookmarks(content_type, content_id);

-- Enable bookmarks module by default
UPDATE tenant_config SET modules = modules || '{"bookmarks": true}'::jsonb;
