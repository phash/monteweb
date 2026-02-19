ALTER TABLE tenant_config ADD COLUMN available_languages TEXT[] NOT NULL DEFAULT '{de,en}';
