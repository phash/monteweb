-- V106: Add impersonation module toggle (disabled by default)
UPDATE tenant_config SET modules = modules || '{"impersonation": false}'::jsonb
WHERE NOT (modules ? 'impersonation');
