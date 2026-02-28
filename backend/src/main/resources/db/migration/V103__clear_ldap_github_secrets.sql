-- DSGVO H-03: Force re-entry of secrets after AES-GCM encryption deployment
-- Existing plaintext values cannot be migrated without the runtime encryption key
UPDATE tenant_config SET ldap_bind_password = NULL;
UPDATE tenant_config SET github_pat = NULL;
