CREATE TABLE tenant_config (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_name VARCHAR(255) NOT NULL,
    logo_url    VARCHAR(500),
    theme       JSONB NOT NULL DEFAULT '{}',
    modules     JSONB NOT NULL DEFAULT '{"messaging": false, "files": false, "jobboard": false, "cleaning": false}',
    target_hours_per_family NUMERIC(5,1) NOT NULL DEFAULT 20.0,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
