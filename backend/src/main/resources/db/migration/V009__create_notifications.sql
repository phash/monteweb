CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type            VARCHAR(50) NOT NULL,   -- POST, COMMENT, MESSAGE, SYSTEM, REMINDER
    title           VARCHAR(300) NOT NULL,
    message         TEXT,
    link            VARCHAR(500),           -- deep-link into the app, e.g. /rooms/{id}
    reference_type  VARCHAR(50),            -- FEED_POST, COMMENT, CONVERSATION, ROOM, etc.
    reference_id    UUID,
    is_read         BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read) WHERE is_read = false;
CREATE INDEX idx_notifications_created ON notifications(created_at DESC);
