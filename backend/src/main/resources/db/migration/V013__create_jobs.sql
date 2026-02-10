CREATE TABLE jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(300) NOT NULL,
    description     TEXT,
    category        VARCHAR(100) NOT NULL,
    location        VARCHAR(200),
    section_id      UUID REFERENCES school_sections(id),
    estimated_hours DECIMAL(5,2) NOT NULL DEFAULT 1.0,
    max_assignees   INTEGER NOT NULL DEFAULT 1,
    status          VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    scheduled_date  DATE,
    scheduled_time  VARCHAR(50),
    created_by      UUID NOT NULL REFERENCES users(id),
    contact_info    VARCHAR(300),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    closed_at       TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_jobs_status ON jobs(status);
CREATE INDEX idx_jobs_category ON jobs(category);
CREATE INDEX idx_jobs_section ON jobs(section_id);
CREATE INDEX idx_jobs_scheduled ON jobs(scheduled_date);
CREATE INDEX idx_jobs_created_by ON jobs(created_by);
