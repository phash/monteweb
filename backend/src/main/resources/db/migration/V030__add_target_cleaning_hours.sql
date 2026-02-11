-- Add configurable cleaning hours target and update default total hours
ALTER TABLE tenant_config ADD COLUMN target_cleaning_hours NUMERIC(5,1) NOT NULL DEFAULT 3.0;
UPDATE tenant_config SET target_hours_per_family = 30.0;
