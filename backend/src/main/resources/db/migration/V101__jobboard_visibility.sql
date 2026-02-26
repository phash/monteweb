-- Issue #12: Jobboard enhancements - visibility, approval workflow, own jobs
-- Add visibility column for job visibility control (PRIVATE, DRAFT, PUBLIC)
ALTER TABLE jobs ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC';

-- Add approval tracking columns
ALTER TABLE jobs ADD COLUMN approved_by UUID REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE jobs ADD COLUMN approved_at TIMESTAMP WITH TIME ZONE;

-- Index for filtering by visibility
CREATE INDEX idx_jobs_visibility ON jobs(visibility);
