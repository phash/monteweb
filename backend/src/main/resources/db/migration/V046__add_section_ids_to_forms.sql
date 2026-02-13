-- Add section_ids array column for multi-section targeting
ALTER TABLE forms ADD COLUMN IF NOT EXISTS section_ids UUID[];

-- Migrate existing SECTION-scoped forms: copy scope_id into section_ids array
UPDATE forms SET section_ids = ARRAY[scope_id]
WHERE scope = 'SECTION' AND scope_id IS NOT NULL AND section_ids IS NULL;

CREATE INDEX idx_forms_section_ids ON forms USING gin(section_ids);
