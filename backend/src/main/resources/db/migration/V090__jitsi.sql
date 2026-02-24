-- Jitsi video conferencing integration
ALTER TABLE tenant_config ADD COLUMN jitsi_enabled BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE tenant_config ADD COLUMN jitsi_server_url VARCHAR(300) DEFAULT 'https://meet.jit.si';
ALTER TABLE calendar_events ADD COLUMN jitsi_room_name VARCHAR(200);
ALTER TABLE rooms ADD COLUMN jitsi_room_name VARCHAR(200);
