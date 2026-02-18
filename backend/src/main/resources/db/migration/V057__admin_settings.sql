-- Admin settings: multilanguage toggle, default language, user approval toggle
ALTER TABLE tenant_config ADD COLUMN multilanguage_enabled BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE tenant_config ADD COLUMN default_language VARCHAR(5) NOT NULL DEFAULT 'de';
ALTER TABLE tenant_config ADD COLUMN require_user_approval BOOLEAN NOT NULL DEFAULT true;
