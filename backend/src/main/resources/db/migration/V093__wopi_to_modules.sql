-- Move wopi_enabled from dedicated column into modules JSONB map
UPDATE tenant_config
SET modules = modules || jsonb_build_object('wopi', wopi_enabled);

-- Drop the dedicated column (now managed via modules map)
ALTER TABLE tenant_config DROP COLUMN wopi_enabled;
