CREATE TABLE rooms (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    type            VARCHAR(50) NOT NULL, -- KLASSE, GRUPPE, PROJEKT, INTEREST, CUSTOM
    section_id      UUID REFERENCES school_sections(id),
    settings        JSONB NOT NULL DEFAULT '{"chatEnabled": false, "filesEnabled": false, "parentSpaceEnabled": false, "visibility": "MEMBERS_ONLY"}',
    is_archived     BOOLEAN NOT NULL DEFAULT false,
    archive_at      TIMESTAMP WITH TIME ZONE,
    created_by      UUID NOT NULL REFERENCES users(id),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE room_members (
    room_id     UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(30) NOT NULL DEFAULT 'MEMBER', -- LEADER, MEMBER, PARENT_MEMBER, GUEST
    joined_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (room_id, user_id)
);

CREATE INDEX idx_rooms_section ON rooms(section_id);
CREATE INDEX idx_rooms_type ON rooms(type);
CREATE INDEX idx_rooms_archived ON rooms(is_archived) WHERE is_archived = false;
CREATE INDEX idx_room_members_user ON room_members(user_id);
