CREATE TABLE job_assignments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id          UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id),
    family_id       UUID NOT NULL REFERENCES families(id),
    status          VARCHAR(30) NOT NULL DEFAULT 'ASSIGNED',
    actual_hours    DECIMAL(5,2),
    confirmed       BOOLEAN NOT NULL DEFAULT false,
    confirmed_by    UUID REFERENCES users(id),
    confirmed_at    TIMESTAMP WITH TIME ZONE,
    notes           TEXT,
    assigned_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    started_at      TIMESTAMP WITH TIME ZONE,
    completed_at    TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_job_assignments_job ON job_assignments(job_id);
CREATE INDEX idx_job_assignments_user ON job_assignments(user_id);
CREATE INDEX idx_job_assignments_family ON job_assignments(family_id);
CREATE INDEX idx_job_assignments_status ON job_assignments(status);
CREATE UNIQUE INDEX idx_job_assignments_unique ON job_assignments(job_id, user_id);
