-- Widen totp_secret column to accommodate AES-GCM encrypted values
ALTER TABLE users ALTER COLUMN totp_secret TYPE VARCHAR(256);
