-- DSGVO/GDPR Compliance: Soft-delete, consent, terms, data access logging, privacy policy

-- 1. Soft-delete fields on users (14-day grace period)
ALTER TABLE users ADD COLUMN deletion_requested_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN scheduled_deletion_at TIMESTAMPTZ;
CREATE INDEX idx_users_scheduled_deletion ON users (scheduled_deletion_at) WHERE scheduled_deletion_at IS NOT NULL;

-- 2. Consent records (photo consent, chat consent for minors)
CREATE TABLE consent_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    granted_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    consent_type VARCHAR(50) NOT NULL,
    granted BOOLEAN NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at TIMESTAMPTZ,
    notes TEXT
);
CREATE INDEX idx_consent_user ON consent_records (user_id, consent_type);

-- 3. Terms acceptances (versioned)
CREATE TABLE terms_acceptances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    terms_version VARCHAR(20) NOT NULL,
    accepted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ip_address VARCHAR(45)
);
CREATE UNIQUE INDEX idx_terms_user_version ON terms_acceptances (user_id, terms_version);

-- 4. Data access log (Art. 15 DSGVO audit trail)
CREATE TABLE data_access_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    accessed_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(50) NOT NULL,
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_data_access_target ON data_access_log (target_user_id, created_at);

-- 5. Privacy policy + terms on tenant_config
ALTER TABLE tenant_config ADD COLUMN privacy_policy_text TEXT;
ALTER TABLE tenant_config ADD COLUMN privacy_policy_version VARCHAR(20) DEFAULT '1.0';
ALTER TABLE tenant_config ADD COLUMN terms_text TEXT;
ALTER TABLE tenant_config ADD COLUMN terms_version VARCHAR(20) DEFAULT '1.0';
ALTER TABLE tenant_config ADD COLUMN data_retention_days_notifications INTEGER DEFAULT 90;
ALTER TABLE tenant_config ADD COLUMN data_retention_days_audit INTEGER DEFAULT 1095;
