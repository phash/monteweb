-- Test-specific configuration overrides
-- These run AFTER all production migrations to set up a test-friendly environment

-- Disable user approval requirement so TestHelper.registerAndGetToken() returns tokens immediately
UPDATE tenant_config SET require_user_approval = false;
