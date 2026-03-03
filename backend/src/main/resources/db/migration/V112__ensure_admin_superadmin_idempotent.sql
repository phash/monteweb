-- V112: Idempotent fix — always ensure admin@monteweb.local has SUPERADMIN role
-- This migration exists because V111 ran once but the role can be changed
-- by test runs or admin API calls. V112 ensures the role is correct on every fresh DB start.
UPDATE users
SET role = 'SUPERADMIN'
WHERE email = 'admin@monteweb.local' AND role != 'SUPERADMIN';
