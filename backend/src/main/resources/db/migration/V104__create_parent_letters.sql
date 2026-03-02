-- V104: Create parent letters module tables
-- Tables: parent_letters, parent_letter_recipients, parent_letter_configs

-- 1. parent_letters: the letter itself (per KLASSE room, enforced in code)
CREATE TABLE parent_letters (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id       UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    created_by    UUID          REFERENCES users(id) ON DELETE SET NULL,
    title         VARCHAR(300)  NOT NULL,
    content       TEXT          NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    send_date     TIMESTAMP WITH TIME ZONE,
    deadline      TIMESTAMP WITH TIME ZONE,
    reminder_days INTEGER       NOT NULL DEFAULT 3,
    reminder_sent BOOLEAN       NOT NULL DEFAULT false,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_parent_letters_room_id    ON parent_letters(room_id);
CREATE INDEX idx_parent_letters_created_by ON parent_letters(created_by);
CREATE INDEX idx_parent_letters_status     ON parent_letters(status);

-- 2. parent_letter_recipients: per-recipient tracking
CREATE TABLE parent_letter_recipients (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    letter_id        UUID NOT NULL REFERENCES parent_letters(id) ON DELETE CASCADE,
    student_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    family_id        UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,
    status           VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    read_at          TIMESTAMP WITH TIME ZONE,
    confirmed_at     TIMESTAMP WITH TIME ZONE,
    confirmed_by     UUID REFERENCES users(id) ON DELETE SET NULL,
    reminder_sent_at TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_plr_letter_id  ON parent_letter_recipients(letter_id);
CREATE INDEX idx_plr_parent_id  ON parent_letter_recipients(parent_id);
CREATE INDEX idx_plr_student_id ON parent_letter_recipients(student_id);
CREATE INDEX idx_plr_status     ON parent_letter_recipients(status);

ALTER TABLE parent_letter_recipients
    ADD CONSTRAINT unique_plr_letter_parent UNIQUE (letter_id, parent_id, student_id);

-- 3. parent_letter_configs: section-level or global configuration
CREATE TABLE parent_letter_configs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    section_id          UUID REFERENCES school_sections(id) ON DELETE CASCADE,
    letterhead_path     VARCHAR(500),
    signature_template  TEXT,
    reminder_days       INTEGER NOT NULL DEFAULT 3,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- One config per section (non-null section_ids must be unique)
CREATE UNIQUE INDEX unique_plc_section ON parent_letter_configs(section_id)
    WHERE section_id IS NOT NULL;

-- Only one global config is allowed (where section_id IS NULL)
CREATE UNIQUE INDEX unique_plc_global ON parent_letter_configs((true))
    WHERE section_id IS NULL;
