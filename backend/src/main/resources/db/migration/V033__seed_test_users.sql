-- V033: Seed test users for each role
-- Password for all test users: test1234
-- Hash generated via Spring BCryptPasswordEncoder
-- IMPORTANT: Remove or disable these accounts in production!

INSERT INTO users (id, email, password_hash, first_name, last_name, display_name, role, is_active, email_verified, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'sectionadmin@monteweb.local',
     '$2a$10$dQa6248nzH.S5yYnSpGfr.b7BnuGroDZ/G18yp.vzoAx45FQ7hecO',
     'Sarah', 'Bereichs', 'Sarah Bereichs', 'SECTION_ADMIN', true, true, NOW(), NOW()),

    (gen_random_uuid(), 'lehrer@monteweb.local',
     '$2a$10$dQa6248nzH.S5yYnSpGfr.b7BnuGroDZ/G18yp.vzoAx45FQ7hecO',
     'Thomas', 'Lehmann', 'Thomas Lehmann', 'TEACHER', true, true, NOW(), NOW()),

    (gen_random_uuid(), 'eltern@monteweb.local',
     '$2a$10$dQa6248nzH.S5yYnSpGfr.b7BnuGroDZ/G18yp.vzoAx45FQ7hecO',
     'Maria', 'Elterlich', 'Maria Elterlich', 'PARENT', true, true, NOW(), NOW()),

    (gen_random_uuid(), 'schueler@monteweb.local',
     '$2a$10$dQa6248nzH.S5yYnSpGfr.b7BnuGroDZ/G18yp.vzoAx45FQ7hecO',
     'Lukas', 'Schultze', 'Lukas Schultze', 'STUDENT', true, true, NOW(), NOW())

ON CONFLICT (email) DO NOTHING;
