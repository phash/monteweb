-- Phase 5: Room chat channels (links rooms to conversations)

CREATE TABLE room_chat_channels (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id         UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    channel_type    VARCHAR(30) NOT NULL DEFAULT 'MAIN', -- MAIN, PARENTS, STUDENTS
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (room_id, channel_type)
);

CREATE INDEX idx_room_chat_channels_room ON room_chat_channels(room_id);
CREATE INDEX idx_room_chat_channels_conversation ON room_chat_channels(conversation_id);
