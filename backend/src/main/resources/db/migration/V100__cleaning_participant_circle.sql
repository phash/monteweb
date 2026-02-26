-- V100: Add participant circle restriction to cleaning configs
-- Allows restricting who can register for a cleaning slot (by room or section)
ALTER TABLE cleaning_configs ADD COLUMN participant_circle VARCHAR(20) DEFAULT 'SECTION';
ALTER TABLE cleaning_configs ADD COLUMN participant_circle_id UUID;

-- Add endpoint for updating actual minutes after checkout
ALTER TABLE cleaning_registrations ADD COLUMN duration_confirmed BOOLEAN DEFAULT FALSE;
