-- Test-specific configuration overrides
-- These run AFTER all production migrations to set up a test-friendly environment

-- Disable user approval requirement so TestHelper.registerAndGetToken() returns tokens immediately
UPDATE tenant_config SET require_user_approval = false;

-- Enable parent-to-parent and student-to-student messaging for integration tests
UPDATE tenant_config SET parent_to_parent_messaging = true;
UPDATE tenant_config SET student_to_student_messaging = true;

-- Set admin@monteweb.local password to 'test1234' (BCrypt hash) for test loginAs() helpers
UPDATE users SET password_hash = '$2a$10$dQa6248nzH.S5yYnSpGfr.b7BnuGroDZ/G18yp.vzoAx45FQ7hecO'
WHERE email = 'admin@monteweb.local';

-- Create a family for eltern@monteweb.local so jobboard apply tests work
-- (JobboardService.applyForJob requires user to belong to a family)
INSERT INTO families (id, name) VALUES ('00000000-0000-0000-0000-ffffffffffff', 'Familie Elterlich');
INSERT INTO family_members (family_id, user_id, role)
SELECT '00000000-0000-0000-0000-ffffffffffff', id, 'PARENT'
FROM users WHERE email = 'eltern@monteweb.local';
