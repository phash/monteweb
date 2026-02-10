CREATE TABLE cleaning_configs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    section_id      UUID NOT NULL REFERENCES school_sections(id),
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    day_of_week     INTEGER NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    min_participants INTEGER NOT NULL DEFAULT 2,
    max_participants INTEGER NOT NULL DEFAULT 4,
    hours_credit    DECIMAL(5,2) NOT NULL DEFAULT 2.0,
    active          BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_cleaning_configs_section ON cleaning_configs(section_id);
CREATE INDEX idx_cleaning_configs_active ON cleaning_configs(active);
