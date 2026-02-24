-- Calendar event colors
ALTER TABLE calendar_events ADD COLUMN color VARCHAR(7);

-- Section calendar colors (admin configurable)
ALTER TABLE tenant_config ADD COLUMN calendar_colors JSONB;
