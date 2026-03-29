-- Allow anonymization of creator references when users are deleted (DSGVO Art. 17)
ALTER TABLE tasks ALTER COLUMN created_by DROP NOT NULL;
ALTER TABLE wiki_pages ALTER COLUMN created_by DROP NOT NULL;
ALTER TABLE wiki_pages ALTER COLUMN last_edited_by DROP NOT NULL;
ALTER TABLE wiki_page_versions ALTER COLUMN edited_by DROP NOT NULL;
