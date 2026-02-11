CREATE TABLE cleaning_slots (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_id       UUID NOT NULL REFERENCES cleaning_configs(id),
    section_id      UUID NOT NULL REFERENCES school_sections(id),
    slot_date       DATE NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    min_participants INTEGER NOT NULL DEFAULT 2,
    max_participants INTEGER NOT NULL DEFAULT 4,
    status          VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    qr_token        VARCHAR(500),
    cancelled       BOOLEAN NOT NULL DEFAULT false,
    cancel_reason   VARCHAR(500),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE cleaning_registrations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slot_id         UUID NOT NULL REFERENCES cleaning_slots(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id),
    user_name       VARCHAR(200) NOT NULL DEFAULT '',
    family_id       UUID NOT NULL REFERENCES families(id),
    checked_in      BOOLEAN NOT NULL DEFAULT false,
    check_in_at     TIMESTAMP WITH TIME ZONE,
    checked_out     BOOLEAN NOT NULL DEFAULT false,
    check_out_at    TIMESTAMP WITH TIME ZONE,
    actual_minutes  INTEGER,
    no_show         BOOLEAN NOT NULL DEFAULT false,
    swap_offered    BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_cleaning_slots_date ON cleaning_slots(slot_date);
CREATE INDEX idx_cleaning_slots_section ON cleaning_slots(section_id);
CREATE INDEX idx_cleaning_slots_status ON cleaning_slots(status);
CREATE INDEX idx_cleaning_slots_config ON cleaning_slots(config_id);
CREATE INDEX idx_cleaning_registrations_slot ON cleaning_registrations(slot_id);
CREATE INDEX idx_cleaning_registrations_user ON cleaning_registrations(user_id);
CREATE INDEX idx_cleaning_registrations_family ON cleaning_registrations(family_id);
CREATE UNIQUE INDEX idx_cleaning_registrations_unique ON cleaning_registrations(slot_id, user_id);
