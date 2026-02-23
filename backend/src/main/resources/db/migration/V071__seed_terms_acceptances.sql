-- Pre-populate terms_acceptances for all existing active users with the current
-- terms version. This prevents existing users from being prompted to accept
-- terms they were already implicitly using before the DSGVO migration.
INSERT INTO terms_acceptances (user_id, terms_version, accepted_at)
SELECT u.id, tc.terms_version, now()
FROM users u
CROSS JOIN tenant_config tc
WHERE u.deletion_requested_at IS NULL
  AND tc.terms_version IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM terms_acceptances ta
      WHERE ta.user_id = u.id AND ta.terms_version = tc.terms_version
  );
