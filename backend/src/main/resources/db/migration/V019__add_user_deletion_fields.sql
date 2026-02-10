-- DSGVO: Add fields for user account deletion/anonymization
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN deletion_reason TEXT;
