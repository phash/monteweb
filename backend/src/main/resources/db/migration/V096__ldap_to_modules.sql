-- Move ldap_enabled into modules JSONB map
UPDATE tenant_config
SET modules = modules || jsonb_build_object('ldap', ldap_enabled);

ALTER TABLE tenant_config DROP COLUMN ldap_enabled;
