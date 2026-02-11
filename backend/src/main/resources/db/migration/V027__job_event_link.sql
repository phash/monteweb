ALTER TABLE jobs ADD COLUMN event_id UUID;
ALTER TABLE jobs ADD CONSTRAINT fk_jobs_event
    FOREIGN KEY (event_id) REFERENCES calendar_events(id) ON DELETE SET NULL;
CREATE INDEX idx_jobs_event_id ON jobs(event_id);
