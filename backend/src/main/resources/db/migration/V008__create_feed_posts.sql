CREATE TABLE feed_posts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id       UUID NOT NULL REFERENCES users(id),
    title           VARCHAR(300),
    content         TEXT NOT NULL,
    source_type     VARCHAR(30) NOT NULL,  -- ROOM, SECTION, SCHOOL, BOARD, SYSTEM
    source_id       UUID,                  -- room_id or section_id (nullable for SCHOOL/SYSTEM)
    is_pinned       BOOLEAN NOT NULL DEFAULT false,
    is_parent_only  BOOLEAN NOT NULL DEFAULT false,
    published_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at      TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE feed_post_attachments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     UUID NOT NULL REFERENCES feed_posts(id) ON DELETE CASCADE,
    file_name   VARCHAR(500) NOT NULL,
    file_url    VARCHAR(1000) NOT NULL,
    file_type   VARCHAR(100),
    file_size   BIGINT,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE feed_post_comments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     UUID NOT NULL REFERENCES feed_posts(id) ON DELETE CASCADE,
    author_id   UUID NOT NULL REFERENCES users(id),
    content     TEXT NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_feed_posts_source ON feed_posts(source_type, source_id);
CREATE INDEX idx_feed_posts_author ON feed_posts(author_id);
CREATE INDEX idx_feed_posts_published ON feed_posts(published_at DESC);
CREATE INDEX idx_feed_posts_pinned ON feed_posts(is_pinned) WHERE is_pinned = true;
CREATE INDEX idx_feed_post_comments_post ON feed_post_comments(post_id);
CREATE INDEX idx_feed_post_attachments_post ON feed_post_attachments(post_id);
