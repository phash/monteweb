-- Move directory_admin_only into modules JSONB map
UPDATE tenant_config
SET modules = modules || jsonb_build_object('directoryAdminOnly', directory_admin_only);

ALTER TABLE tenant_config DROP COLUMN directory_admin_only;
