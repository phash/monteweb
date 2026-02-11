-- OIDC/SSO fields for users
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE users ADD COLUMN oidc_provider VARCHAR(50);
ALTER TABLE users ADD COLUMN oidc_subject  VARCHAR(255);

CREATE UNIQUE INDEX idx_users_oidc ON users(oidc_provider, oidc_subject)
    WHERE oidc_provider IS NOT NULL AND oidc_subject IS NOT NULL;
