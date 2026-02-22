-- DSGVO: Make user-reference columns nullable for data cleanup/anonymization

-- messages.sender_id: anonymize sent messages on user deletion
ALTER TABLE messages ALTER COLUMN sender_id DROP NOT NULL;

-- calendar_events.created_by: anonymize events on user deletion
ALTER TABLE calendar_events ALTER COLUMN created_by DROP NOT NULL;

-- jobs.created_by: anonymize jobs on user deletion
ALTER TABLE jobs ALTER COLUMN created_by DROP NOT NULL;

-- fotobox_threads.created_by: anonymize threads on user deletion
ALTER TABLE fotobox_threads ALTER COLUMN created_by DROP NOT NULL;
