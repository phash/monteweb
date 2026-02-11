-- V032: Seed default SUPERADMIN account
-- Email: admin@monteweb.local / Password: admin123
-- IMPORTANT: Change the password after first login!
INSERT INTO users (id, email, password_hash, first_name, last_name, display_name, role, is_active, email_verified, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'admin@monteweb.local',
    '$2a$10$mreOAA6R8NlXg6aN6cthPOPH0kc1pP5akMUrlZ3q7H00f6rFYdJ9y',
    'System',
    'Admin',
    'System Admin',
    'SUPERADMIN',
    true,
    true,
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;
