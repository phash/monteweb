-- Add sole custody support to families
ALTER TABLE families ADD COLUMN sole_custody BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE families ADD COLUMN sole_custody_approved BOOLEAN NOT NULL DEFAULT false;

-- Add family configuration to tenant_config
ALTER TABLE tenant_config ADD COLUMN sole_custody_enabled BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE tenant_config ADD COLUMN require_family_switch_approval BOOLEAN NOT NULL DEFAULT false;
