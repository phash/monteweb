-- Issue #78: Calendar event type for filtering (e.g. CLEANING events)
ALTER TABLE calendar_events ADD COLUMN event_type VARCHAR(30) NOT NULL DEFAULT 'GENERAL';

-- Issue #80: Room-scoped jobs
ALTER TABLE jobs ADD COLUMN room_id UUID REFERENCES rooms(id) ON DELETE SET NULL;
CREATE INDEX idx_jobs_room_id ON jobs(room_id) WHERE room_id IS NOT NULL;
