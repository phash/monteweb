# Issues Batch #151–#154 Design

## Goal

Fix the critical SuperAdmin 403 bug, restrict email visibility in the directory, add a "partially assigned" job status, and rework the Admin Settings UI with grouped Accordion layout.

## Issue #153 — SuperAdmin 403-Fehler in Räumen (Bug)

**Root Cause:** `FileService`, `RoomChatService`, and `CalendarService` check room membership via `roomModuleApi.isUserInRoom()` without bypassing for SUPERADMIN. The Fotobox module already handles this correctly.

**Fix:** Add SUPERADMIN check before room membership verification in three services:
- `FileService.requireRoomMembership()` — inject `UserModuleApi`, check `user.role() == SUPERADMIN`
- `RoomChatService.getChannels()` and `getOrCreateChannel()` — add SUPERADMIN check via `UserModuleApi`
- `CalendarService.getRoomEvents()` — add SUPERADMIN check (already has `userModule` dependency)

No frontend changes needed.

## Issue #151 — Email nur für Admin/Teacher im Verzeichnis

**Problem:** `UserInfo` DTO always includes `email`. The directory and search endpoints expose emails to all authenticated users.

**Fix:** In `UserService`, create a `toUserInfoFiltered(User user, UserRole callerRole)` method that sets email to `null` when the caller is not TEACHER, SECTION_ADMIN, or SUPERADMIN. Apply to:
- `GET /users/directory` — pass caller's role into the mapping
- `GET /users/search` — same
- `GET /users/{id}` — same

Frontend: `DirectoryView.vue` already handles `null` email (no display). No frontend changes expected.

## Issue #154 — Jobbörse "teilweise vergeben"

**Problem:** Job status jumps from OPEN to ASSIGNED only when all slots are filled. No intermediate state.

**Fix:**
- Add `PARTIALLY_ASSIGNED` to `JobStatus` enum (no DB migration needed — VARCHAR column)
- `JobboardService.applyForJob()`: set status to `PARTIALLY_ASSIGNED` when `0 < currentAssignees < maxAssignees`
- `JobboardService.cancelAssignment()`: recalculate status — back to `OPEN` if 0 assignees, `PARTIALLY_ASSIGNED` if partial
- Frontend: add type, i18n key (`'Teilweise vergeben'` / `'Partially Assigned'`), severity `'warn'`

## Issue #152 — Admin Settings UI Rework

**Problem:** 12+ flat settings sections on one page, no visual grouping.

**Fix:** Replace flat layout with PrimeVue Accordion, grouped logically:

| Group | Icon | Contains |
|-------|------|----------|
| Allgemein | `pi-cog` | Language, Registration, Bundesland/Vacations |
| Kommunikation | `pi-comments` | Messaging rules, Directory, Jobboard settings, Family |
| Integrationen | `pi-link` | Jitsi, WOPI, ClamAV, LDAP |
| Sicherheit | `pi-shield` | 2FA, Maintenance Mode |
| Stunden | `pi-clock` | Hours configuration |

Each group gets an icon header. Settings logic (store, API) remains unchanged — only template and CSS restructured. Section-specific overrides shown via inline Select within each group.
