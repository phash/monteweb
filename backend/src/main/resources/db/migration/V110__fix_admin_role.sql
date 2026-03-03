-- V110: Ensure admin@monteweb.local has SUPERADMIN role
-- The V107 seed extended test data creates users with ON CONFLICT DO NOTHING,
-- but the admin role may be wrong if the account was created before V032 could set it.
UPDATE users SET role = 'SUPERADMIN' WHERE email = 'admin@monteweb.local';
