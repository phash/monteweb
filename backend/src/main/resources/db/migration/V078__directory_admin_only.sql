-- #116: Allow SuperAdmin to restrict user directory to admins only
ALTER TABLE tenant_config ADD COLUMN directory_admin_only BOOLEAN NOT NULL DEFAULT false;
