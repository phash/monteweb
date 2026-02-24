ALTER TABLE tenant_config ADD COLUMN clamav_enabled BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE tenant_config ADD COLUMN clamav_host VARCHAR(200) DEFAULT 'clamav';
ALTER TABLE tenant_config ADD COLUMN clamav_port INTEGER DEFAULT 3310;
