ALTER TABLE cleaning_configs ADD COLUMN room_id UUID REFERENCES rooms(id) ON DELETE SET NULL;
CREATE INDEX idx_cleaning_configs_room ON cleaning_configs(room_id) WHERE room_id IS NOT NULL;
