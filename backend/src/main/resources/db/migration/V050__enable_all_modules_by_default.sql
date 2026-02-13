-- V050: Enable all modules by default so menu items are visible after login
-- Fixes GitHub Issue #10: "Fehlende Inhalte im Menü"
UPDATE tenant_config
SET modules = '{"messaging": true, "files": true, "jobboard": true, "cleaning": true, "calendar": true, "forms": true, "fotobox": true}'::jsonb;
