-- Add confirmation fields to cleaning_registrations (mirroring jobboard pattern)
ALTER TABLE cleaning_registrations
    ADD COLUMN confirmed BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN confirmed_by UUID,
    ADD COLUMN confirmed_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_cleaning_registrations_confirmed ON cleaning_registrations(confirmed);
