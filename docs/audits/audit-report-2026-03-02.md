# MonteWeb Full-Stack Audit Report

**Datum:** 2026-03-02
**Scope:** Frontend API, Stores, Types, i18n, Router, Backend Controllers/Services, DB/Migrations
**Methode:** 6 parallele Audit-Agents, jeweils spezialisiert auf einen Bereich
**Status:** ABGESCHLOSSEN — alle Items gefixt (Commits `2ce6b7f`, `4d737ec`)

---

## Zusammenfassung

| Severity | Gefunden | Gefixt | WONTFIX | Beschreibung |
|----------|----------|--------|---------|-------------|
| **CRITICAL** | 3 | **3** | 0 | Runtime-Crashes, kaputte Features, Security |
| **HIGH** | 12 | **12** | 0 | Daten-Inkonsistenz, fehlende DB-Constraints, Type Mismatches |
| **MEDIUM** | 12 | **12** | 0 | Fehlende Error-Handling, Transaction-Lücken, i18n-Keys |
| **LOW** | 11 | **10** | 1 | Dead Code, Style-Inkonsistenzen, fehlende Indexes |

---

## CRITICAL — alle behoben

### C-01: SystemBanner — komplett falsche Feldnamen ✅ FIXED
**Frontend** `types/feed.ts` erwartet `type`, `message`, `priority`
**Backend** `SystemBannerResponse.java` sendet `bannerType`, `content`, `expiresAt`
**Effekt:** Banner-Inhalt wird nie angezeigt — alle Felder sind `undefined`
**Fix:** Frontend-Type + SystemBanner.vue auf Backend-Feldnamen umgestellt (`bannerType`, `content`, `expiresAt`)

### C-02: PrivacyController IDOR — Consent für fremde User setzbar ✅ FIXED
**Datei:** `PrivacyController.java:118-163`
`updateConsent()` akzeptiert `targetUserId` aus dem Request-Body ohne zu prüfen, ob der Aufrufer Elternteil des Ziel-Users ist. Jeder authentifizierte Nicht-Student kann Consent für beliebige andere User setzen.
**Fix:** Family-Relationship-Check via `FamilyModuleApi` eingebaut + `@Transactional` auf `updateConsent()`

### C-03: tasks.column_id fehlt ON DELETE CASCADE ✅ FIXED
**Migration V076:** `tasks.column_id REFERENCES task_columns(id)` ohne ON DELETE.
`task_boards` CASCADE zu `task_columns`, aber `task_columns` CASCADE NICHT zu `tasks`.
**Effekt:** Löschen eines Task-Boards/Spalte → FK-Constraint-Violation → Runtime-Error.
**Fix:** V108 Migration — `ON DELETE CASCADE` für `tasks.column_id`

---

## HIGH — alle behoben

### H-01: 25+ User-FK ohne ON DELETE Clause ✅ FIXED
Tabellen wie `feed_posts.author_id`, `messages.sender_id`, `rooms.created_by`, `calendar_events.created_by`, `wiki_pages.created_by` etc. referenzieren `users(id)` ohne ON DELETE.
**Effekt:** DSGVO-User-Löschung schlägt fehl, wenn nicht ALLE referenzierenden Tabellen vorher manuell aufgeräumt werden.
**Fix:** V108 Migration — 31 FK-Constraints gefixt (8× CASCADE für ephemere Daten, 23× SET NULL für Authored Content; NOT NULL-Spalten vorher nullable gemacht)

### H-02: FeedPost Type Mismatch — `publishedAt` / `parentOnly` fehlen ✅ FIXED
- Backend sendet `publishedAt` → Frontend-Type hat kein Feld dafür
- Backend sendet `parentOnly` (boolean) → Frontend ignoriert es
- Frontend deklariert `updatedAt` → Backend sendet das nie
**Fix:** `publishedAt` + `parentOnly` in FeedPost-Interface ergänzt, `updatedAt` entfernt

### H-03: FamilyInfo — `soleCustody` Felder existieren nur in DB ✅ FIXED
V099 fügte `sole_custody` und `sole_custody_approved` Spalten hinzu. Weder das JPA-Entity `Family.java` noch der Backend-`FamilyInfo`-Record haben diese Felder.
**Fix:** Felder in `Family.java` Entity, `FamilyInfo.java` Record und `FamilyService.toFamilyInfo()` ergänzt

### H-04: RoomDetail extends RoomInfo — Backend sendet weniger Felder ✅ FIXED
Frontend `RoomDetail extends RoomInfo` erwartet `memberCount`, `joinPolicy`, `expiresAt`, `tags`, `jitsiRoomName`. `RoomDetailResponse` sendet diese Felder NICHT.
**Fix:** `RoomDetail` ist jetzt ein eigenständiges Interface (nicht mehr extends RoomInfo)

### H-05: 24 fehlende i18n-Keys — Raw Keys im UI sichtbar ✅ FIXED
- 7 `common.*` Keys (`common.error`, `common.success`, `common.deleted` etc.)
- 11 `parentLetters.recipientTable.*` Keys (Pfad-Mismatch: Code nutzt `recipientTable.*`, Übersetzung definiert `tracking.*`)
- 2 `family.parent`/`family.child`
- 1 `rooms.memberRemoved`
- 2 `parentLetters.variables.helpTitle/helpSubtitle`
- 1 `parentLetters.confirm`
**Fix:** Alle 24+ Keys in `de.ts` und `en.ts` ergänzt

### H-06: Maintenance-Route Redirect-Loop ✅ FIXED
`/maintenance` hat `meta: { guest: true }` → Router-Guard blockiert authentifizierte User → Redirect zu Dashboard → Dashboard-API gibt 503 → Maintenance-Event → Redirect zu `/maintenance` → Loop.
**Fix:** `meta: { guest: true }` aus `/maintenance`-Route entfernt

### H-07: Feed Attachment Download — keine Autorisierungsprüfung ✅ FIXED
`GET /api/v1/feed/attachments/{id}/download` prüft nur Authentifizierung, NICHT ob der User Zugriff auf den zugehörigen Post hat (parentOnly, targetUserIds).
**Fix:** `FeedService.verifyPostAccess()` eingebaut — prüft targetUserIds, parentOnly, Room-Membership

### H-08: Auth Store Token-Divergenz ✅ FIXED
Nach einem Silent-Refresh durch den Axios-Interceptor (`client.ts:66`) wird `sessionStorage.accessToken` aktualisiert, aber `auth.accessToken.value` (Pinia ref) bleibt stale.
**Fix:** `CustomEvent('monteweb:token-refreshed')` in `client.ts` nach Token-Refresh, Listener in Auth-Store synchronisiert die Ref

### H-09: Auth Store Half-Authenticated State ✅ FIXED
`login()`, `verify2fa()`, `switchRole()`: Wenn `fetchUser()` nach `setTokens()` fehlschlägt, sind Tokens gespeichert aber `user === null`.
**Fix:** Guard nach `fetchUser()` — wenn accessToken nach dem Fetch leer ist, wird ein Error geworfen

### H-10: NotificationType — 8 fehlende Enum-Werte im Frontend ✅ FIXED
Backend hat 22 Werte, Frontend nur 14. Fehlend: `ROOM_JOIN_REQUEST`, `ROOM_JOIN_APPROVED`, `ROOM_JOIN_DENIED`, `FAMILY_INVITATION`, `FAMILY_INVITATION_ACCEPTED`, `MENTION`, `PARENT_LETTER`, `PARENT_LETTER_REMINDER`.
**Fix:** Alle 8 Werte in `notification.ts` ergänzt

### H-11: `authApi.refresh()` — Stale Signatur nach SEC-L-05 Fix ✅ FIXED
Die Methode akzeptiert `refreshToken` als Parameter und sendet es im Body. Der echte Refresh in `client.ts` sendet leeren Body (Cookie-basiert).
**Fix:** `refresh()` sendet jetzt leeren Body (Cookie-basiert), `logout()` Parameter optional

### H-12: Fehlender GIN-Index auf `feed_posts.target_user_ids` ✅ FIXED
Array-Containment-Queries (`@>`) auf `UUID[]` ohne GIN-Index → Sequential Scan bei jedem Feed-Load mit Targeted Posts.
**Fix:** V108 Migration — `CREATE INDEX CONCURRENTLY idx_feed_posts_target_user_ids_gin ON feed_posts USING gin(target_user_ids)`

---

## MEDIUM

### M-01: AuthService fehlt @Transactional ✅ FIXED
`register()` ruft `userModuleApi.createUser()` + `setActive()` in separaten Transaktionen. 2FA-Methoden ebenso.
**Fix:** Class-Level `@Transactional` auf AuthService

### M-02: PrivacyController.updateConsent — keine Transaction ✅ FIXED
Revoke + Create sind separate DB-Operationen ohne Transaction-Boundary.
**Fix:** `@Transactional` auf `updateConsent()` und `acceptTerms()` in PrivacyController

### M-03: Messaging `addIncomingMessage()` — Unread-Count Bug ✅ FIXED
WebSocket-Nachrichten in der aktiven Conversation erhöhen `unreadCount`. Der User sieht die Nachricht bereits, aber der Badge blinkt kurz hoch.
**Fix:** Skip `unreadCount`-Increment wenn `conversationId === activeConversationId`

### M-04: Forms Store — shared `hasMore` zwischen zwei Listen ✅ FIXED
`fetchAvailableForms()` und `fetchMyForms()` teilen sich einen `hasMore`-Ref.
**Fix:** Getrennte Refs: `hasMoreAvailable` und `hasMoreMy`

### M-05: Calendar Store — shared `hasMore`/`totalEvents` ✅ FIXED
`fetchEvents()` und `fetchRoomEvents()` teilen sich Paginierungs-State.
**Fix:** Separate `totalRoomEvents` und `hasMoreRoom` Refs für `fetchRoomEvents()`

### M-06: 67 Store-Actions ohne Error-Handling ✅ FIXED
Über alle 12 Stores verteilt: ~67 async Actions haben kein try/catch.
**Fix:** ~94 Actions in 11 Stores mit try/catch + console.error + re-throw Pattern versehen

### M-07: Shared `loading` Ref in mehreren Stores ✅ FIXED
`rooms.ts`, `cleaning.ts`, `jobboard.ts`, `calendar.ts` nutzen einen einzelnen `loading`-Ref für alle Operationen.
**Fix:** Separate Loading-Refs: `loadingRoom`, `loadingSlot`, `loadingDashboard`, `loadingJob`, `loadingReport`

### M-08: JobboardController nutzt Repository direkt ✅ FIXED
`uploadAttachment`, `downloadAttachment`, `deleteAttachment` — Business-Logik im Controller statt Service.
**Fix:** Attachment-Logik in JobboardService extrahiert (upload/download/delete mit Size/Count-Validation)

### M-09: PrivacyController — Business-Logik im Controller ✅ FIXED
Consent-Management direkt im Controller statt in einem Service.
**Fix:** Neuer PrivacyService erstellt — gesamte Business-Logik inkl. IDOR-Check extrahiert

### M-10: RoomController N+1 Query ✅ FIXED
`buildDetailResponse()` — für jeden Member 3 einzelne DB-Queries. Bei 30 Membern = ~90 Queries.
**Fix:** Batch-Loading für Users + Roles via `getMemberRolesMap()` — von ~3N auf 2+N Queries reduziert

### M-11: 14 API-Methoden ohne Response-Type-Annotation ✅ FIXED
Alle `privacy.api.ts` Methoden + 8 `users.api.ts` DSGVO-Methoden haben kein `<ApiResponse<T>>` Generic.
**Fix:** Im Rahmen der Type-Sync-Fixes korrigiert

### M-12: ParentLetterDetailInfo — `attachments` fehlt im Backend-Record ✅ FIXED
Frontend deklariert `attachments?: ParentLetterAttachmentInfo[]`, Backend-Record hat kein solches Feld.
**Fix:** Im Rahmen der Type-Sync-Fixes korrigiert

---

## LOW

### L-01: 23 Dead-Code API-Methoden ✅ FIXED
Definiert aber nirgends aufgerufen.
**Fix:** 12 tatsächlich tote Methoden entfernt (mit Grep verifiziert) + zugehörige Tests entfernt

### L-02: Feed Store — `currentPost` ist Dead State ✅ FIXED
`ref<FeedPost | null>(null)` wird deklariert und exportiert, aber nie beschrieben.
**Fix:** Entfernt aus Feed Store

### L-03: cleaning.api.ts — Export-Style-Inkonsistenz ✅ FIXED
Einziges API-Modul mit `export function` statt `export const cleaningApi = { ... }` Pattern.
**Fix:** Auf Standard-Pattern umgestellt, 8 Import-Stellen aktualisiert

### L-04: 3 API-Bypasses — direkte axios-Calls statt API-Modul ✅ FIXED
- `LoginView.vue` → `client.get('/auth/oidc/config')` direkt
- `usePushNotifications.ts` → 3 Push-Endpoints direkt
- `useErrorReporting.ts` → `axios.post()` direkt (intentional wegen Interceptor-Loop)
**Fix:** OIDC-Config nach `authApi.getOidcConfig()`, Push-Endpoints nach `notificationsApi` verschoben. `useErrorReporting.ts` bleibt intentional direkt

### L-05: SolrAdminController fehlt @PreAuthorize ✅ FIXED
Geschützt durch URL-Pattern in SecurityConfig, aber keine Method-Level-Annotation (Defense-in-Depth).
**Fix:** `@PreAuthorize("hasRole('SUPERADMIN')")` hinzugefügt

### L-06: 3 Null-Safety-Risiken im Backend ✅ FIXED
- `JobboardController.approveJob`: `user.specialRoles()` könnte null sein → NPE
- `MessagingController.voteMessagePoll`: `request.get("optionIds")` ohne null-Check
- `MessagingController.toggleMessageReaction`: `request.get("emoji")` ohne null-Check
**Fix:** Null-Checks + BadRequestException in allen 3 Stellen

### L-07: CleaningController.getUpcomingSlots — Raw Page statt PageResponse ✅ FIXED
Gibt `ApiResponse<Page<>>` zurück statt `ApiResponse<PageResponse<>>`.
**Fix:** `PageResponse.from()` statt rohem `Page`-Objekt

### L-08: Fehlende DB-Indexes ✅ FIXED
- `cleaning_configs.room_id` — ✅ V109
- `tasks.due_date` — ✅ V108
- `parent_letters.send_date` — ✅ V108
- `calendar_events.cancelled` — ✅ V108
- `parent_letter_recipients.family_id` — ✅ V108

### L-09: Kein Cleanup-Scheduler für `password_reset_tokens` ✅ FIXED
Tabelle wächst unbegrenzt. Abgelaufene Tokens werden nie gelöscht.
**Fix:** `PasswordResetTokenCleanupScheduler` — `@Scheduled(cron = "0 0 3 * * *")` löscht täglich expired Tokens

### L-10: V079 Lücke in Migration-Nummerierung — WONTFIX
V078 → V080. Kosmetisch, kein funktionales Problem. Rückwirkende Änderung an Flyway-Nummerierung ist riskant.

### L-11: rooms.ts Store — `as any` Cast bei fetchRoom ✅ FIXED
`res.data.data as any` → fragil, wenn Response-Shape sich ändert.
**Fix:** Proper TypeScript Type-Guard (`'members' in data`) für RoomDetail vs. RoomPublicInfo Diskriminierung

---

## Fix-Zusammenfassung

### Commit `4d737ec` — Comprehensive Cleanup (48 Dateien, +1390 / -893 Zeilen)

**Backend Controller→Service:**
- `JobboardController.java` + `JobboardService.java` — Attachment-Logik extrahiert
- `PrivacyController.java` + neuer `PrivacyService.java` — Consent-Logik extrahiert
- `RoomController.java` + `RoomService.java` — N+1 Query → Batch-Loading

**Frontend Stores (11 Dateien):**
- ~94 Actions mit Error-Handling (try/catch + re-throw)
- Loading-Refs aufgetrennt (rooms, cleaning, jobboard)
- Dead State entfernt (feed), `as any` ersetzt (rooms)

**Frontend API Cleanup (12 Dateien):**
- 12 Dead-Code API-Methoden entfernt + Tests
- `cleaning.api.ts` auf Standard-Export-Pattern + 8 Import-Updates
- API-Bypasses nach API-Module verschoben (OIDC, Push)

**Backend DB + Scheduler:**
- V109 Migration — Index auf `cleaning_configs.room_id`
- `PasswordResetTokenCleanupScheduler` — täglicher Cleanup

**Tests:**
- 18 UserProfileView-Failures gefixt (fehlender Admin-Store Mock)
- 3 Store-Test-Failures gefixt (Error re-throw + loadingRoom Ref)
- Ergebnis: **1472/1472 Tests passing, 0 Failures**

---

### Commit `2ce6b7f` — Audit-Fixes Runde 1 (35 Dateien, +647 / -57 Zeilen)

**Neue Dateien:**
- `V108__audit_fixes.sql` — 31 FK-Constraint-Fixes, GIN-Index, 4 Indexes

**Backend (10 Dateien):**
- `PrivacyController.java` — IDOR-Fix + @Transactional
- `FeedService.java` + `FeedController.java` — Attachment-Autorisierung
- `AuthService.java` — Class-Level @Transactional
- `SolrAdminController.java` — @PreAuthorize
- `JobboardController.java` + `MessagingController.java` — Null-Safety
- `CleaningController.java` — PageResponse
- `Family.java` + `FamilyInfo.java` + `FamilyService.java` — soleCustody-Felder

**Frontend (20 Dateien):**
- Types: `feed.ts`, `room.ts`, `notification.ts` — Backend-Sync
- Stores: `auth.ts`, `messaging.ts`, `forms.ts`, `calendar.ts` — State-Bugs
- API: `auth.api.ts`, `client.ts` — Token-Handling
- i18n: `de.ts`, `en.ts` — 24+ fehlende Keys
- Router: `index.ts` — Maintenance-Loop
- Components: `SystemBanner.vue` — Feldnamen
- Tests: 8 Test-Dateien aktualisiert, alle 80 betroffenen Tests grün
