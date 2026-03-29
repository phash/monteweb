# Database Schema Reference

Flyway V001-V115 (114 migrations). **Never modify existing migrations** -- always create new `VXXX__description.sql`. Hibernate `ddl-auto: validate`.

## Conventions

- UUID PKs (`DEFAULT gen_random_uuid()`), `TIMESTAMP WITH TIME ZONE`
- PostgreSQL arrays (`TEXT[]`, `UUID[]`), JSONB for flexible data
- All timestamps as `Instant` in Java

## Key Tables & Gotchas

### Users
- `users.force_password_change`: BOOLEAN default false, set by CSV import
- `users.totp_secret`: VARCHAR(256), AES-256-GCM encrypted (legacy plaintext auto-handled by `AesEncryptionService.decrypt()`)
- `users.dark_mode`: VARCHAR, three modes (SYSTEM/LIGHT/DARK)

### Rooms
- `room_members`: composite PK `(room_id, user_id)` -- **no `id` column**
- `rooms.is_archived` (NOT `archived` -- use the correct column name)
- `room_folders.audience`: VARCHAR(20) default `'ALL'` -- visibility: ALL, PARENTS_ONLY, STUDENTS_ONLY

### Feed
- `feed_posts.target_user_ids`: `UUID[]` -- NULL=visible to all, filled=only listed users
- `feed_post_attachments`: id, post_id (FK), file_name, file_url (MinIO path), file_type, file_size, sort_order, created_at

### Messaging
- `messages.reply_to_id`: UUID FK for reply threading (ON DELETE SET NULL)
- `messages.content`: nullable (image-only messages)
- `message_images`: MinIO storage with thumbnails, 90-day auto-cleanup

### Cleaning
- `cleaning_configs.specific_date`: optional DATE for one-time Putzaktionen
- `cleaning_configs.calendar_event_id` + `cleaning_configs.job_id`: links Putzaktion to calendar event and job

### Family & Billing
- `families.is_hours_exempt`: BOOLEAN default false, exempts family from Elternstunden
- `families.is_active`: BOOLEAN default true, family deactivation support
- `billing_periods`: family billing with year/month/status (OPEN/CLOSED) -- Jahresabrechnung

### Tenant Config
- `tenant_config.bundesland`: VARCHAR(5) default `'BY'`, determines public holidays
- `tenant_config.school_vacations`: JSONB array of `{name, from, to}`
- `tenant_config.github_repo` + `tenant_config.github_pat`: GitHub integration for error reports (PAT encrypted with AES)
- `tenant_config.require_assignment_confirmation`: BOOLEAN default true
- `tenant_config.available_languages`: TEXT[] default `'{de,en}'`
- `tenant_config.modules`: JSONB map of all feature toggles (core modules + DB-managed toggles)

### Forms
- `forms.section_ids`: `UUID[]` with GIN index -- multi-section targeting for SECTION-scoped forms

### Error Reports
- `error_reports`: fingerprint-based dedup, status (NEW/REPORTED/RESOLVED/IGNORED), `github_issue_url`, occurrence tracking

### Fundgrube
- `fundgrube_items`: lost & found with section filter, claim workflow (expires +24h)
- `fundgrube_images`: MinIO storage with thumbnails

### Tasks & Wiki
- `task_boards` + `task_columns` + `tasks`: per-room kanban (V076), boards unique per room, columns ordered by position
- `wiki_pages` + `wiki_page_versions`: per-room wiki (V077), slug unique per room, self-referencing parent_id

### Bookmarks & Profile Fields
- `bookmarks`: user bookmarks for posts, events, jobs, wiki pages (type + target_id)
- `profile_field_definitions` + `profile_field_values`: custom profile fields defined by admins, values per user

### Fotobox
- `fotobox_threads.audience`: VARCHAR(20) default `'ALL'` -- same visibility as folders
