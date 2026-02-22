-- Test data: disable user approval so registerAndGetToken works
UPDATE tenant_config SET require_user_approval = false;
