CREATE TABLE family_invitations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id   UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,
    inviter_id  UUID NOT NULL REFERENCES users(id),
    invitee_id  UUID NOT NULL REFERENCES users(id),
    role        VARCHAR(20) NOT NULL DEFAULT 'PARENT',
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ
);

CREATE INDEX idx_family_invitations_invitee ON family_invitations(invitee_id, status);
CREATE INDEX idx_family_invitations_family ON family_invitations(family_id, status);
