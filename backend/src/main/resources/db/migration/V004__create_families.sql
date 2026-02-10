CREATE TABLE families (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(200) NOT NULL,
    invite_code     VARCHAR(20) UNIQUE,
    invite_expires  TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE family_members (
    family_id   UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(20) NOT NULL, -- PARENT or CHILD
    joined_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (family_id, user_id)
);

CREATE INDEX idx_family_members_user ON family_members(user_id);
CREATE INDEX idx_families_invite_code ON families(invite_code) WHERE invite_code IS NOT NULL;
