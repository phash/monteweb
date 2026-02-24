-- WOPI token table for ONLYOFFICE document editing
CREATE TABLE wopi_tokens (
    token VARCHAR(64) PRIMARY KEY,
    file_id UUID NOT NULL REFERENCES room_files(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    room_id UUID NOT NULL,
    permissions VARCHAR(20) NOT NULL DEFAULT 'EDIT',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_wopi_tokens_expires ON wopi_tokens(expires_at);
CREATE INDEX idx_wopi_tokens_file ON wopi_tokens(file_id);

-- Tenant config for WOPI/ONLYOFFICE
ALTER TABLE tenant_config ADD COLUMN wopi_enabled BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE tenant_config ADD COLUMN wopi_office_url VARCHAR(300);
