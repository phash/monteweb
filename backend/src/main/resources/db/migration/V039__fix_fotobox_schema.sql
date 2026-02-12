-- V039: Fix fotobox schema issues from V038

-- Fix: modules column may be NULL, use COALESCE before appending
UPDATE tenant_config
SET modules = COALESCE(modules, '{}'::jsonb) || '{"fotobox": true}'::jsonb
WHERE NOT (COALESCE(modules, '{}'::jsonb) ? 'fotobox');

-- Fix: Add ON DELETE SET NULL to created_by FK on fotobox_threads (GDPR user deletion)
ALTER TABLE fotobox_threads DROP CONSTRAINT IF EXISTS fotobox_threads_created_by_fkey;
ALTER TABLE fotobox_threads
    ADD CONSTRAINT fotobox_threads_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE fotobox_threads ALTER COLUMN created_by DROP NOT NULL;

-- Fix: Add ON DELETE SET NULL to uploaded_by FK on fotobox_images (GDPR user deletion)
ALTER TABLE fotobox_images DROP CONSTRAINT IF EXISTS fotobox_images_uploaded_by_fkey;
ALTER TABLE fotobox_images
    ADD CONSTRAINT fotobox_images_uploaded_by_fkey
    FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE fotobox_images ALTER COLUMN uploaded_by DROP NOT NULL;
