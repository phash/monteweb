-- Add specific date to cleaning configs (for one-time cleaning actions)
ALTER TABLE cleaning_configs ADD COLUMN specific_date DATE;

-- Add bundesland and school vacations to tenant config
ALTER TABLE tenant_config ADD COLUMN bundesland VARCHAR(5) NOT NULL DEFAULT 'BY';
ALTER TABLE tenant_config ADD COLUMN school_vacations JSONB NOT NULL DEFAULT '[]';
