-- Make feed_posts.content nullable (poll-only posts)
ALTER TABLE feed_posts ALTER COLUMN content DROP NOT NULL;

-- Inline polls for feed posts
CREATE TABLE feed_polls (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     UUID NOT NULL REFERENCES feed_posts(id) ON DELETE CASCADE,
    question    VARCHAR(500) NOT NULL,
    multiple    BOOLEAN NOT NULL DEFAULT false,
    closes_at   TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_feed_polls_post UNIQUE (post_id)
);

CREATE TABLE feed_poll_options (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id     UUID NOT NULL REFERENCES feed_polls(id) ON DELETE CASCADE,
    label       VARCHAR(200) NOT NULL,
    position    INT NOT NULL DEFAULT 0
);

CREATE TABLE feed_poll_votes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    option_id   UUID NOT NULL REFERENCES feed_poll_options(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_feed_poll_vote UNIQUE (option_id, user_id)
);

CREATE INDEX idx_feed_poll_options_poll ON feed_poll_options(poll_id);
CREATE INDEX idx_feed_poll_votes_option ON feed_poll_votes(option_id);
CREATE INDEX idx_feed_poll_votes_user ON feed_poll_votes(user_id);

-- Inline polls for chat messages
CREATE TABLE message_polls (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id      UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    question        VARCHAR(500) NOT NULL,
    multiple        BOOLEAN NOT NULL DEFAULT false,
    closes_at       TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_message_polls_message UNIQUE (message_id)
);

CREATE TABLE message_poll_options (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id     UUID NOT NULL REFERENCES message_polls(id) ON DELETE CASCADE,
    label       VARCHAR(200) NOT NULL,
    position    INT NOT NULL DEFAULT 0
);

CREATE TABLE message_poll_votes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    option_id   UUID NOT NULL REFERENCES message_poll_options(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_message_poll_vote UNIQUE (option_id, user_id)
);

CREATE INDEX idx_message_poll_options_poll ON message_poll_options(poll_id);
CREATE INDEX idx_message_poll_votes_option ON message_poll_votes(option_id);
CREATE INDEX idx_message_poll_votes_user ON message_poll_votes(user_id);
