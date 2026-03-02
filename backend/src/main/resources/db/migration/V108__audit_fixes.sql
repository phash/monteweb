-- V108: Database audit fixes — FK ON DELETE clauses, missing indexes
-- Addresses: C-03, H-01, H-12, L-08

-- ============================================================================
-- Section: C-03 — tasks.column_id missing ON DELETE CASCADE
-- ============================================================================
ALTER TABLE tasks DROP CONSTRAINT tasks_column_id_fkey;
ALTER TABLE tasks ADD CONSTRAINT tasks_column_id_fkey
    FOREIGN KEY (column_id) REFERENCES task_columns(id) ON DELETE CASCADE;


-- ============================================================================
-- Section: H-01 — Add ON DELETE to user FK references
-- ============================================================================

-- --------------------------------------------------------------------------
-- H-01a: NOT NULL user FKs where CASCADE is appropriate (ephemeral/user-specific data)
-- --------------------------------------------------------------------------

-- password_reset_tokens.user_id (V020) — NOT NULL, ephemeral per-user token
ALTER TABLE password_reset_tokens DROP CONSTRAINT password_reset_tokens_user_id_fkey;
ALTER TABLE password_reset_tokens ADD CONSTRAINT password_reset_tokens_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- room_subscriptions.user_id (V037) — NOT NULL, per-user feed preference
ALTER TABLE room_subscriptions DROP CONSTRAINT room_subscriptions_user_id_fkey;
ALTER TABLE room_subscriptions ADD CONSTRAINT room_subscriptions_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- calendar_event_rsvps.user_id (V025) — NOT NULL, per-user RSVP
ALTER TABLE calendar_event_rsvps DROP CONSTRAINT calendar_event_rsvps_user_id_fkey;
ALTER TABLE calendar_event_rsvps ADD CONSTRAINT calendar_event_rsvps_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- form_response_tracking.user_id (V028) — NOT NULL (composite PK), tracking record
ALTER TABLE form_response_tracking DROP CONSTRAINT form_response_tracking_user_id_fkey;
ALTER TABLE form_response_tracking ADD CONSTRAINT form_response_tracking_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- cleaning_registrations.user_id (V016) — NOT NULL, per-user registration
ALTER TABLE cleaning_registrations DROP CONSTRAINT cleaning_registrations_user_id_fkey;
ALTER TABLE cleaning_registrations ADD CONSTRAINT cleaning_registrations_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- job_assignments.user_id (V014) — NOT NULL, per-user assignment
ALTER TABLE job_assignments DROP CONSTRAINT job_assignments_user_id_fkey;
ALTER TABLE job_assignments ADD CONSTRAINT job_assignments_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- room_join_requests.user_id (V034) — NOT NULL, per-user request
ALTER TABLE room_join_requests DROP CONSTRAINT room_join_requests_user_id_fkey;
ALTER TABLE room_join_requests ADD CONSTRAINT room_join_requests_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- form_responses.user_id (V028) — already nullable, SET NULL
ALTER TABLE form_responses DROP CONSTRAINT form_responses_user_id_fkey;
ALTER TABLE form_responses ADD CONSTRAINT form_responses_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;

-- --------------------------------------------------------------------------
-- H-01b: Columns that need DROP NOT NULL first, then ON DELETE SET NULL
--         (authored/owned content that should survive user deletion)
-- --------------------------------------------------------------------------

-- rooms.created_by (V005) — NOT NULL, make nullable then SET NULL
ALTER TABLE rooms ALTER COLUMN created_by DROP NOT NULL;
ALTER TABLE rooms DROP CONSTRAINT rooms_created_by_fkey;
ALTER TABLE rooms ADD CONSTRAINT rooms_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL;

-- feed_posts.author_id (V008) — NOT NULL, make nullable then SET NULL
ALTER TABLE feed_posts ALTER COLUMN author_id DROP NOT NULL;
ALTER TABLE feed_posts DROP CONSTRAINT feed_posts_author_id_fkey;
ALTER TABLE feed_posts ADD CONSTRAINT feed_posts_author_id_fkey
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE SET NULL;

-- feed_post_comments.author_id (V008) — NOT NULL, make nullable then SET NULL
ALTER TABLE feed_post_comments ALTER COLUMN author_id DROP NOT NULL;
ALTER TABLE feed_post_comments DROP CONSTRAINT feed_post_comments_author_id_fkey;
ALTER TABLE feed_post_comments ADD CONSTRAINT feed_post_comments_author_id_fkey
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE SET NULL;

-- conversations.created_by (V010) — NOT NULL, make nullable then SET NULL
ALTER TABLE conversations ALTER COLUMN created_by DROP NOT NULL;
ALTER TABLE conversations DROP CONSTRAINT conversations_created_by_fkey;
ALTER TABLE conversations ADD CONSTRAINT conversations_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL;

-- messages.sender_id (V010, nullable since V069) — just add ON DELETE SET NULL
ALTER TABLE messages DROP CONSTRAINT messages_sender_id_fkey;
ALTER TABLE messages ADD CONSTRAINT messages_sender_id_fkey
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE SET NULL;

-- room_folders.created_by (V011) — NOT NULL, make nullable then SET NULL
ALTER TABLE room_folders ALTER COLUMN created_by DROP NOT NULL;
ALTER TABLE room_folders DROP CONSTRAINT room_folders_created_by_fkey;
ALTER TABLE room_folders ADD CONSTRAINT room_folders_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL;

-- room_files.uploaded_by (V011) — NOT NULL, make nullable then SET NULL
ALTER TABLE room_files ALTER COLUMN uploaded_by DROP NOT NULL;
ALTER TABLE room_files DROP CONSTRAINT room_files_uploaded_by_fkey;
ALTER TABLE room_files ADD CONSTRAINT room_files_uploaded_by_fkey
    FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE SET NULL;

-- jobs.created_by (V013, nullable since V069) — just add ON DELETE SET NULL
ALTER TABLE jobs DROP CONSTRAINT jobs_created_by_fkey;
ALTER TABLE jobs ADD CONSTRAINT jobs_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL;

-- job_assignments.confirmed_by (V014) — already nullable, SET NULL
ALTER TABLE job_assignments DROP CONSTRAINT job_assignments_confirmed_by_fkey;
ALTER TABLE job_assignments ADD CONSTRAINT job_assignments_confirmed_by_fkey
    FOREIGN KEY (confirmed_by) REFERENCES users(id) ON DELETE SET NULL;

-- room_discussion_threads.created_by (V022) — NOT NULL, make nullable then SET NULL
ALTER TABLE room_discussion_threads ALTER COLUMN created_by DROP NOT NULL;
ALTER TABLE room_discussion_threads DROP CONSTRAINT room_discussion_threads_created_by_fkey;
ALTER TABLE room_discussion_threads ADD CONSTRAINT room_discussion_threads_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL;

-- room_discussion_replies.author_id (V022) — NOT NULL, make nullable then SET NULL
ALTER TABLE room_discussion_replies ALTER COLUMN author_id DROP NOT NULL;
ALTER TABLE room_discussion_replies DROP CONSTRAINT room_discussion_replies_author_id_fkey;
ALTER TABLE room_discussion_replies ADD CONSTRAINT room_discussion_replies_author_id_fkey
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE SET NULL;

-- calendar_events.created_by (V025, nullable since V069) — just add ON DELETE SET NULL
ALTER TABLE calendar_events DROP CONSTRAINT calendar_events_created_by_fkey;
ALTER TABLE calendar_events ADD CONSTRAINT calendar_events_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL;

-- forms.created_by (V028) — NOT NULL, make nullable then SET NULL
ALTER TABLE forms ALTER COLUMN created_by DROP NOT NULL;
ALTER TABLE forms DROP CONSTRAINT forms_created_by_fkey;
ALTER TABLE forms ADD CONSTRAINT forms_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL;

-- room_join_requests.resolved_by (V034) — already nullable, SET NULL
ALTER TABLE room_join_requests DROP CONSTRAINT room_join_requests_resolved_by_fkey;
ALTER TABLE room_join_requests ADD CONSTRAINT room_join_requests_resolved_by_fkey
    FOREIGN KEY (resolved_by) REFERENCES users(id) ON DELETE SET NULL;

-- family_invitations.inviter_id (V035) — NOT NULL, make nullable then SET NULL
ALTER TABLE family_invitations ALTER COLUMN inviter_id DROP NOT NULL;
ALTER TABLE family_invitations DROP CONSTRAINT family_invitations_inviter_id_fkey;
ALTER TABLE family_invitations ADD CONSTRAINT family_invitations_inviter_id_fkey
    FOREIGN KEY (inviter_id) REFERENCES users(id) ON DELETE SET NULL;

-- family_invitations.invitee_id (V035) — NOT NULL, make nullable then SET NULL
ALTER TABLE family_invitations ALTER COLUMN invitee_id DROP NOT NULL;
ALTER TABLE family_invitations DROP CONSTRAINT family_invitations_invitee_id_fkey;
ALTER TABLE family_invitations ADD CONSTRAINT family_invitations_invitee_id_fkey
    FOREIGN KEY (invitee_id) REFERENCES users(id) ON DELETE SET NULL;

-- billing_periods.closed_by (V047) — already nullable, SET NULL
ALTER TABLE billing_periods DROP CONSTRAINT billing_periods_closed_by_fkey;
ALTER TABLE billing_periods ADD CONSTRAINT billing_periods_closed_by_fkey
    FOREIGN KEY (closed_by) REFERENCES users(id) ON DELETE SET NULL;

-- job_attachments.uploaded_by (V064) — NOT NULL, make nullable then SET NULL
ALTER TABLE job_attachments ALTER COLUMN uploaded_by DROP NOT NULL;
ALTER TABLE job_attachments DROP CONSTRAINT job_attachments_uploaded_by_fkey;
ALTER TABLE job_attachments ADD CONSTRAINT job_attachments_uploaded_by_fkey
    FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE SET NULL;

-- tasks.created_by (V076) — NOT NULL, make nullable then SET NULL
ALTER TABLE tasks ALTER COLUMN created_by DROP NOT NULL;
ALTER TABLE tasks DROP CONSTRAINT tasks_created_by_fkey;
ALTER TABLE tasks ADD CONSTRAINT tasks_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL;

-- wiki_pages.created_by (V077) — NOT NULL, make nullable then SET NULL
ALTER TABLE wiki_pages ALTER COLUMN created_by DROP NOT NULL;
ALTER TABLE wiki_pages DROP CONSTRAINT wiki_pages_created_by_fkey;
ALTER TABLE wiki_pages ADD CONSTRAINT wiki_pages_created_by_fkey
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL;

-- wiki_pages.last_edited_by (V077) — already nullable, SET NULL
ALTER TABLE wiki_pages DROP CONSTRAINT wiki_pages_last_edited_by_fkey;
ALTER TABLE wiki_pages ADD CONSTRAINT wiki_pages_last_edited_by_fkey
    FOREIGN KEY (last_edited_by) REFERENCES users(id) ON DELETE SET NULL;

-- wiki_page_versions.edited_by (V077) — NOT NULL, make nullable then SET NULL
ALTER TABLE wiki_page_versions ALTER COLUMN edited_by DROP NOT NULL;
ALTER TABLE wiki_page_versions DROP CONSTRAINT wiki_page_versions_edited_by_fkey;
ALTER TABLE wiki_page_versions ADD CONSTRAINT wiki_page_versions_edited_by_fkey
    FOREIGN KEY (edited_by) REFERENCES users(id) ON DELETE SET NULL;


-- ============================================================================
-- Section: H-12 — GIN index on feed_posts.target_user_ids
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_feed_posts_target_user_ids
    ON feed_posts USING GIN (target_user_ids);


-- ============================================================================
-- Section: L-08 — Missing indexes
-- ============================================================================
-- Note: idx_cleaning_configs_room already exists from V065, skipped here
CREATE INDEX IF NOT EXISTS idx_tasks_due_date
    ON tasks(due_date);

CREATE INDEX IF NOT EXISTS idx_calendar_events_cancelled
    ON calendar_events(cancelled);

CREATE INDEX IF NOT EXISTS idx_parent_letters_send_date
    ON parent_letters(send_date);

CREATE INDEX IF NOT EXISTS idx_parent_letter_recipients_family_id
    ON parent_letter_recipients(family_id);
