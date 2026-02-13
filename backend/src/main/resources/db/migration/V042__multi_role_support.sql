-- V042: Multi-role support
-- Users can have multiple assigned roles and switch between them.
-- The existing 'role' column remains the "active role" used by all permission checks.

ALTER TABLE users ADD COLUMN assigned_roles text[] NOT NULL DEFAULT '{}';

-- Backfill: every existing user gets their current role as assigned role
-- SUPERADMIN and STUDENT keep empty assigned_roles (they are fixed-role users)
UPDATE users SET assigned_roles = ARRAY[role::text]
WHERE role NOT IN ('SUPERADMIN', 'STUDENT');
