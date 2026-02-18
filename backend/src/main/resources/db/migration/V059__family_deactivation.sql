-- Add is_active to families for deactivation support
ALTER TABLE families
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT true;

CREATE INDEX idx_families_is_active ON families(is_active);
