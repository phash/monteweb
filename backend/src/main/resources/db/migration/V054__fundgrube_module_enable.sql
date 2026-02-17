-- V054: Enable fundgrube module by default
UPDATE tenant_config
SET modules = modules || '{"fundgrube": true}'::jsonb;
