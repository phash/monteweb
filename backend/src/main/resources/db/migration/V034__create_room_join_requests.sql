CREATE TABLE room_join_requests (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id     UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id),
    message     TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    resolved_by UUID REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ
);

CREATE INDEX idx_room_join_requests_room ON room_join_requests(room_id, status);
CREATE INDEX idx_room_join_requests_user ON room_join_requests(user_id);
