-- Custom profile field definitions (admin-managed)
CREATE TABLE profile_field_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    field_key VARCHAR(50) NOT NULL UNIQUE,
    label_de VARCHAR(200) NOT NULL,
    label_en VARCHAR(200) NOT NULL,
    field_type VARCHAR(20) NOT NULL,
    options JSONB,
    required BOOLEAN NOT NULL DEFAULT false,
    position INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- User values for custom profile fields
CREATE TABLE profile_field_values (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    field_id UUID NOT NULL REFERENCES profile_field_definitions(id) ON DELETE CASCADE,
    value TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_profile_field_value UNIQUE (user_id, field_id)
);
CREATE INDEX idx_profile_field_values_user ON profile_field_values(user_id);

-- Enable module
UPDATE tenant_config SET modules = modules || '{"profilefields": true}'::jsonb;
