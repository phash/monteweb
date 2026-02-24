-- Email digest user preferences
ALTER TABLE users ADD COLUMN digest_frequency VARCHAR(20) NOT NULL DEFAULT 'NONE';
ALTER TABLE users ADD COLUMN digest_last_sent_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_users_digest ON users(digest_frequency) WHERE digest_frequency != 'NONE';
