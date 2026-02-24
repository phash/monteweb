-- Maintenance Mode
ALTER TABLE tenant_config ADD COLUMN maintenance_enabled BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE tenant_config ADD COLUMN maintenance_message TEXT;
