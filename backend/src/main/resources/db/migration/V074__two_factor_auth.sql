-- V074: Two-Factor Authentication (TOTP)
-- Add TOTP fields to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS totp_secret VARCHAR(64);
ALTER TABLE users ADD COLUMN IF NOT EXISTS totp_enabled BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE users ADD COLUMN IF NOT EXISTS totp_recovery_codes TEXT[];

-- Add 2FA mode to tenant config
ALTER TABLE tenant_config ADD COLUMN IF NOT EXISTS two_factor_mode VARCHAR(20) NOT NULL DEFAULT 'DISABLED';
ALTER TABLE tenant_config ADD COLUMN IF NOT EXISTS two_factor_grace_deadline TIMESTAMP WITH TIME ZONE;
