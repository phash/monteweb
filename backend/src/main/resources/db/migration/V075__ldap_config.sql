-- LDAP/Active Directory integration configuration
ALTER TABLE tenant_config ADD COLUMN ldap_enabled BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE tenant_config ADD COLUMN ldap_url VARCHAR(255);
ALTER TABLE tenant_config ADD COLUMN ldap_base_dn VARCHAR(255);
ALTER TABLE tenant_config ADD COLUMN ldap_bind_dn VARCHAR(255);
ALTER TABLE tenant_config ADD COLUMN ldap_bind_password VARCHAR(512);
ALTER TABLE tenant_config ADD COLUMN ldap_user_search_filter VARCHAR(255) DEFAULT '(uid={0})';
ALTER TABLE tenant_config ADD COLUMN ldap_attr_email VARCHAR(64) DEFAULT 'mail';
ALTER TABLE tenant_config ADD COLUMN ldap_attr_first_name VARCHAR(64) DEFAULT 'givenName';
ALTER TABLE tenant_config ADD COLUMN ldap_attr_last_name VARCHAR(64) DEFAULT 'sn';
ALTER TABLE tenant_config ADD COLUMN ldap_default_role VARCHAR(30) DEFAULT 'PARENT';
ALTER TABLE tenant_config ADD COLUMN ldap_use_ssl BOOLEAN NOT NULL DEFAULT false;
