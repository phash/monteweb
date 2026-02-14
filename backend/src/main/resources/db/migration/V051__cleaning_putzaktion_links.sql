-- V051: Add calendar event and job links to cleaning configs (Putzaktion integration)
ALTER TABLE cleaning_configs ADD COLUMN calendar_event_id UUID;
ALTER TABLE cleaning_configs ADD COLUMN job_id UUID;
