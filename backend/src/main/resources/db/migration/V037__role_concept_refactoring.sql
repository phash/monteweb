-- V037: Role concept refactoring
-- - Replace discoverable boolean with join_policy enum
-- - Add room_subscriptions table for feed muting
-- - RoomSettings JSONB extended fields are handled by Hibernate defaults

-- 1. JoinPolicy: replace discoverable boolean with join_policy enum
ALTER TABLE rooms ADD COLUMN join_policy VARCHAR(20) NOT NULL DEFAULT 'REQUEST';
UPDATE rooms SET join_policy = CASE WHEN discoverable = true THEN 'OPEN' ELSE 'REQUEST' END;
ALTER TABLE rooms DROP COLUMN discoverable;

-- 2. Room subscriptions for feed muting
CREATE TABLE room_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    room_id UUID NOT NULL REFERENCES rooms(id),
    feed_muted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, room_id)
);
CREATE INDEX idx_room_subscriptions_user ON room_subscriptions(user_id);
CREATE INDEX idx_room_subscriptions_room ON room_subscriptions(room_id);
CREATE INDEX idx_room_subscriptions_user_muted ON room_subscriptions(user_id) WHERE feed_muted = true;
