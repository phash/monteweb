-- Reactions on feed posts and comments
CREATE TABLE feed_reactions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     UUID REFERENCES feed_posts(id) ON DELETE CASCADE,
    comment_id  UUID REFERENCES feed_post_comments(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    emoji       VARCHAR(10) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT chk_feed_reaction_target CHECK (
        (post_id IS NOT NULL AND comment_id IS NULL) OR
        (post_id IS NULL AND comment_id IS NOT NULL)
    ),
    CONSTRAINT uq_feed_reaction UNIQUE (post_id, comment_id, user_id, emoji)
);

CREATE INDEX idx_feed_reactions_post ON feed_reactions(post_id) WHERE post_id IS NOT NULL;
CREATE INDEX idx_feed_reactions_comment ON feed_reactions(comment_id) WHERE comment_id IS NOT NULL;
CREATE INDEX idx_feed_reactions_user ON feed_reactions(user_id);

-- Reactions on chat messages
CREATE TABLE message_reactions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id  UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    emoji       VARCHAR(10) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_message_reaction UNIQUE (message_id, user_id, emoji)
);

CREATE INDEX idx_message_reactions_message ON message_reactions(message_id);
CREATE INDEX idx_message_reactions_user ON message_reactions(user_id);
