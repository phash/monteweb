-- Spring Modulith 2.0 schema changes for event_publication table
ALTER TABLE event_publication ADD COLUMN IF NOT EXISTS completion_attempts INTEGER DEFAULT 0;
ALTER TABLE event_publication ADD COLUMN IF NOT EXISTS last_resubmission_date TIMESTAMP WITH TIME ZONE;
ALTER TABLE event_publication ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'PROCESSING';
ALTER TABLE event_publication ADD COLUMN IF NOT EXISTS event_type TEXT;

-- Backfill event_type for any existing rows (set to empty string since we can't infer it)
UPDATE event_publication SET event_type = 'java.lang.Object' WHERE event_type IS NULL;

-- Backfill status for any existing rows
UPDATE event_publication SET status = 'COMPLETED' WHERE completion_date IS NOT NULL AND status IS NULL;
UPDATE event_publication SET status = 'PROCESSING' WHERE completion_date IS NULL AND status IS NULL;

-- Now make event_type NOT NULL
ALTER TABLE event_publication ALTER COLUMN event_type SET NOT NULL;
