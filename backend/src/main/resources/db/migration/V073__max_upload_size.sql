-- V073: Add max upload size configuration to tenant_config
ALTER TABLE tenant_config ADD COLUMN IF NOT EXISTS max_upload_size_mb INTEGER NOT NULL DEFAULT 50;
