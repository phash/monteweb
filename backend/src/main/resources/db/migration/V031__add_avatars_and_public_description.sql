-- V031: Add avatar support for rooms and families, public description for rooms
ALTER TABLE rooms ADD COLUMN avatar_url VARCHAR(500);
ALTER TABLE rooms ADD COLUMN public_description TEXT;
ALTER TABLE families ADD COLUMN avatar_url VARCHAR(500);
