-- V109: Add missing index on cleaning_configs.room_id (full index, non-partial)

CREATE INDEX IF NOT EXISTS idx_cleaning_configs_room_id ON cleaning_configs(room_id);
