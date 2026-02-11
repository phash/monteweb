UPDATE tenant_config SET modules = modules || '{"calendar": false}'::jsonb;
