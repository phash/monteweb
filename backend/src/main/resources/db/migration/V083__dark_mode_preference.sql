-- Dark mode user preference: SYSTEM (follow OS), LIGHT, DARK
ALTER TABLE users ADD COLUMN dark_mode VARCHAR(10) NOT NULL DEFAULT 'SYSTEM';
