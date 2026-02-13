CREATE TABLE error_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fingerprint VARCHAR(64) NOT NULL UNIQUE,
    source VARCHAR(10) NOT NULL,
    error_type VARCHAR(500),
    message TEXT NOT NULL,
    stack_trace TEXT,
    location VARCHAR(1000),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    user_agent TEXT,
    request_url VARCHAR(2000),
    occurrence_count INT NOT NULL DEFAULT 1,
    first_seen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    github_issue_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_error_reports_status ON error_reports(status);
CREATE INDEX idx_error_reports_source ON error_reports(source);
CREATE INDEX idx_error_reports_last_seen ON error_reports(last_seen_at DESC);

ALTER TABLE tenant_config ADD COLUMN github_repo VARCHAR(200);
ALTER TABLE tenant_config ADD COLUMN github_pat VARCHAR(500);
