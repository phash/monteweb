# MonteWeb Full-Stack Audit Report

**Datum:** 2026-03-02
**Scope:** Frontend API, Stores, Types, i18n, Router, Backend Controllers/Services, DB/Migrations
**Methode:** 6 parallele Audit-Agents, jeweils spezialisiert auf einen Bereich

---

## Zusammenfassung

| Severity | Anzahl | Beschreibung |
|----------|--------|-------------|
| **CRITICAL** | 3 | Runtime-Crashes, kaputte Features, Security |
| **HIGH** | 12 | Daten-Inkonsistenz, fehlende DB-Constraints, Type Mismatches |
| **MEDIUM** | 25+ | Fehlende Error-Handling, Transaction-Lücken, i18n-Keys |
| **LOW** | 30+ | Dead Code, Style-Inkonsistenzen, fehlende Indexes |

---

## CRITICAL (sofort fixen)

### C-01: SystemBanner — komplett falsche Feldnamen
**Frontend** `types/feed.ts` erwartet `type`, `message`, `priority`
**Backend** `SystemBannerResponse.java` sendet `bannerType`, `content`, `expiresAt`
**Effekt:** Banner-Inhalt wird nie angezeigt — alle Felder sind `undefined`

### C-02: PrivacyController IDOR — Consent für fremde User setzbar
**Datei:** `PrivacyController.java:118-163`
`updateConsent()` akzeptiert `targetUserId` aus dem Request-Body ohne zu prüfen, ob der Aufrufer Elternteil des Ziel-Users ist. Jeder authentifizierte Nicht-Student kann Consent für beliebige andere User setzen.

### C-03: tasks.column_id fehlt ON DELETE CASCADE
**Migration V076:** `tasks.column_id REFERENCES task_columns(id)` ohne ON DELETE.
`task_boards` CASCADE zu `task_columns`, aber `task_columns` CASCADE NICHT zu `tasks`.
**Effekt:** Löschen eines Task-Boards/Spalte → FK-Constraint-Violation → Runtime-Error.

---

## HIGH

### H-01: 25+ User-FK ohne ON DELETE Clause
Tabellen wie `feed_posts.author_id`, `messages.sender_id`, `rooms.created_by`, `calendar_events.created_by`, `wiki_pages.created_by` etc. referenzieren `users(id)` ohne ON DELETE.
**Effekt:** DSGVO-User-Löschung schlägt fehl, wenn nicht ALLE referenzierenden Tabellen vorher manuell aufgeräumt werden. V107 Seed handelt das korrekt, aber der Application-Code muss es ebenfalls für jeden neuen Table tun.

### H-02: FeedPost Type Mismatch — `publishedAt` / `parentOnly` fehlen
- Backend sendet `publishedAt` → Frontend-Type hat kein Feld dafür
- Backend sendet `parentOnly` (boolean) → Frontend ignoriert es
- Frontend deklariert `updatedAt` → Backend sendet das nie
- **Effekt:** `updatedAt` ist immer `undefined`, Parent-Only-Visibility kann Frontend-seitig nicht geprüft werden

### H-03: FamilyInfo — `soleCustody` Felder existieren nur in DB
V099 fügte `sole_custody` und `sole_custody_approved` Spalten hinzu. Weder das JPA-Entity `Family.java` noch der Backend-`FamilyInfo`-Record haben diese Felder.
**Effekt:** Sole-Custody-Feature ist komplett tot — Spalten existieren, werden aber nie gelesen/geschrieben.

### H-04: RoomDetail extends RoomInfo — Backend sendet weniger Felder
Frontend `RoomDetail extends RoomInfo` erwartet `memberCount`, `joinPolicy`, `expiresAt`, `tags`, `jitsiRoomName`. `RoomDetailResponse` sendet diese Felder NICHT.
**Effekt:** Diese Felder sind `undefined` in der Detail-Ansicht.

### H-05: 24 fehlende i18n-Keys — Raw Keys im UI sichtbar
- 7 `common.*` Keys (`common.error`, `common.success`, `common.deleted` etc.) — 26 Stellen in 9+ Dateien
- 11 `parentLetters.recipientTable.*` Keys (systematischer Pfad-Mismatch: Code nutzt `recipientTable.*`, Übersetzung definiert `tracking.*`)
- 2 `family.parent`/`family.child` (Code nutzt `family.parent`, definiert als `family.roles.PARENT`)
- 1 `rooms.memberRemoved`
- 2 `parentLetters.variables.helpTitle/helpSubtitle`
- 1 `parentLetters.confirm` (existiert als `parentLetters.confirmAction`)

### H-06: Maintenance-Route Redirect-Loop
`/maintenance` hat `meta: { guest: true }` → Router-Guard blockiert authentifizierte User → Redirect zu Dashboard → Dashboard-API gibt 503 → Maintenance-Event → Redirect zu `/maintenance` → Loop.

### H-07: Feed Attachment Download — keine Autorisierungsprüfung
`GET /api/v1/feed/attachments/{id}/download` prüft nur Authentifizierung, NICHT ob der User Zugriff auf den zugehörigen Post hat (parentOnly, targetUserIds).
**Effekt:** Jeder authentifizierte User kann jedes Attachment per UUID-Rate downloaden.

### H-08: Auth Store Token-Divergenz
Nach einem Silent-Refresh durch den Axios-Interceptor (`client.ts:66`) wird `sessionStorage.accessToken` aktualisiert, aber `auth.accessToken.value` (Pinia ref) bleibt stale. Reaktive UI-Logik, die auf `isAuthenticated` basiert, funktioniert nur zufällig.

### H-09: Auth Store Half-Authenticated State
`login()`, `verify2fa()`, `switchRole()`: Wenn `fetchUser()` nach `setTokens()` fehlschlägt, sind Tokens gespeichert aber `user === null`. `isAuthenticated` ist `true`, aber `isAdmin`/`isTeacher` etc. sind alle `false`.

### H-10: NotificationType — 8 fehlende Enum-Werte im Frontend
Backend hat 22 Werte, Frontend nur 14. Fehlend: `ROOM_JOIN_REQUEST`, `ROOM_JOIN_APPROVED`, `ROOM_JOIN_DENIED`, `FAMILY_INVITATION`, `FAMILY_INVITATION_ACCEPTED`, `MENTION`, `PARENT_LETTER`, `PARENT_LETTER_REMINDER`.

### H-11: `authApi.refresh()` — Stale Signatur nach SEC-L-05 Fix
Die Methode akzeptiert `refreshToken` als Parameter und sendet es im Body. Der echte Refresh in `client.ts` sendet leeren Body (Cookie-basiert). Wenn `authApi.refresh()` je aufgerufen würde, würde es nicht funktionieren.

### H-12: Fehlender GIN-Index auf `feed_posts.target_user_ids`
Array-Containment-Queries (`@>`) auf `UUID[]` ohne GIN-Index → Sequential Scan bei jedem Feed-Load mit Targeted Posts.

---

## MEDIUM

### M-01: AuthService fehlt @Transactional
`register()` ruft `userModuleApi.createUser()` + `setActive()` in separaten Transaktionen. 2FA-Methoden (`setup2fa`, `confirm2fa`, `disable2fa`) ebenso. Fehler zwischen den Calls → inkonsistenter Zustand.

### M-02: PrivacyController.updateConsent — keine Transaction
Revoke + Create sind separate DB-Operationen ohne Transaction-Boundary. Fehler bei Create → altes Consent revoked, neues nicht erstellt.

### M-03: Messaging `addIncomingMessage()` — Unread-Count Bug
WebSocket-Nachrichten in der aktiven Conversation erhöhen `unreadCount`. Der User sieht die Nachricht bereits, aber der Badge blinkt kurz hoch.

### M-04: Forms Store — shared `hasMore` zwischen zwei Listen
`fetchAvailableForms()` und `fetchMyForms()` teilen sich einen `hasMore`-Ref. Wechsel zwischen den Tabs korrumpiert den Paginierungs-State.

### M-05: Calendar Store — shared `hasMore`/`totalEvents`
`fetchEvents()` und `fetchRoomEvents()` teilen sich Paginierungs-State. Cross-View-Navigation führt zu falschen Ergebnissen.

### M-06: 67 Store-Actions ohne Error-Handling
Über alle 12 Stores verteilt: ~67 async Actions haben kein try/catch. Wenn die View ebenfalls nicht catcht → Unhandled Promise Rejection.

### M-07: Shared `loading` Ref in mehreren Stores
`rooms.ts`, `cleaning.ts`, `jobboard.ts`, `calendar.ts` nutzen einen einzelnen `loading`-Ref für alle Operationen. Parallele Calls → `loading` wird zu früh `false`.

### M-08: JobboardController nutzt Repository direkt
`uploadAttachment`, `downloadAttachment`, `deleteAttachment` — Business-Logik (Size-Validation, Count-Check, Content-Type-Detection) im Controller statt Service.

### M-09: PrivacyController — Business-Logik im Controller
Consent-Management (Authorization, DB-Queries, Revocation, Entity-Creation) direkt im Controller statt in einem Service.

### M-10: RoomController N+1 Query
`buildDetailResponse()` — für jeden Member 3 einzelne DB-Queries (`findById`, `getUserRoleInRoom`, `findByUserId`). Bei 30 Membern = ~90 Queries.

### M-11: 14 API-Methoden ohne Response-Type-Annotation
Alle `privacy.api.ts` Methoden + 8 `users.api.ts` DSGVO-Methoden haben kein `<ApiResponse<T>>` Generic → `any`-Propagation.

### M-12: ParentLetterDetailInfo — `attachments` fehlt im Backend-Record
Frontend deklariert `attachments?: ParentLetterAttachmentInfo[]`, Backend-Record hat kein solches Feld.

---

## LOW

### L-01: 23 Dead-Code API-Methoden
Definiert aber nirgends (oder nur in Tests) aufgerufen. Beispiele: `authApi.refresh()`, `feedApi.updatePost()`, `feedApi.deleteAttachment()`, `roomsApi.updateInterestFields()`, `cleaningApi.getSwapOffers()`, `searchApi.reindex()`, `errorReportApi.submitReport()`.

### L-02: Feed Store — `currentPost` ist Dead State
`ref<FeedPost | null>(null)` wird deklariert und exportiert, aber nie beschrieben.

### L-03: cleaning.api.ts — Export-Style-Inkonsistenz
Einziges API-Modul mit `export function` statt `export const cleaningApi = { ... }` Pattern.

### L-04: 3 API-Bypasses — direkte axios-Calls statt API-Modul
- `LoginView.vue` → `client.get('/auth/oidc/config')` direkt
- `usePushNotifications.ts` → 3 Push-Endpoints direkt
- `useErrorReporting.ts` → `axios.post()` direkt (intentional wegen Interceptor-Loop)

### L-05: SolrAdminController fehlt @PreAuthorize
Geschützt durch URL-Pattern in SecurityConfig, aber keine Method-Level-Annotation (Defense-in-Depth).

### L-06: 3 Null-Safety-Risiken im Backend
- `JobboardController.approveJob`: `user.specialRoles()` könnte null sein → NPE
- `MessagingController.voteMessagePoll`: `request.get("optionIds")` ohne null-Check
- `MessagingController.toggleMessageReaction`: `request.get("emoji")` ohne null-Check

### L-07: CleaningController.getUpcomingSlots — Raw Page statt PageResponse
Gibt `ApiResponse<Page<>>` zurück statt `ApiResponse<PageResponse<>>`. Frontend bekommt Spring-Page-JSON statt standardisiertes Format.

### L-08: Fehlende DB-Indexes
- `cleaning_configs.room_id` — kein Index
- `tasks.due_date` — kein Index
- `parent_letters.send_date` — kein Index (Scheduler-Performance)
- `calendar_events.cancelled` — kein Index

### L-09: Kein Cleanup-Scheduler für `password_reset_tokens`
Tabelle wächst unbegrenzt. Abgelaufene Tokens werden nie gelöscht.

### L-10: V079 Lücke in Migration-Nummerierung
V078 → V080. Kosmetisch, kein funktionales Problem.

### L-11: rooms.ts Store — `as any` Cast bei fetchRoom
`res.data.data as any` → fragil, wenn Response-Shape sich ändert.

---

## Empfohlene Reihenfolge

### Sprint 1 — Critical & Security
1. **C-01** SystemBanner Feldnamen fixen (Frontend Type + Template)
2. **C-02** PrivacyController IDOR — Family-Relationship-Check einbauen
3. **C-03** Migration: `ON DELETE CASCADE` für `tasks.column_id`
4. **H-07** Feed Attachment Authorization-Check einbauen

### Sprint 2 — Data Integrity & i18n
5. **H-01** Migration: ON DELETE SET NULL/CASCADE für alle User-FKs
6. **H-05** Alle 24 fehlenden i18n-Keys hinzufügen
7. **H-06** Maintenance-Route: `meta: { guest: true }` entfernen
8. **H-12** GIN-Index auf `feed_posts.target_user_ids`

### Sprint 3 — Type Safety & State Management
9. **H-02** FeedPost Type — `publishedAt`, `parentOnly` hinzufügen
10. **H-03** FamilyInfo — `soleCustody` Felder in Entity + Record
11. **H-04** RoomDetail — extends-Kette korrigieren
12. **H-08/H-09** Auth Store Token-Sync + Half-Auth State fixen
13. **M-03** Messaging Unread-Count Bug
14. **M-04/M-05** Separate Paginierungs-State pro Liste

### Sprint 4 — Architecture & Cleanup
15. **M-01/M-02** @Transactional auf AuthService + PrivacyController→Service
16. **M-06** Error-Handling Pattern standardisieren
17. **M-10** RoomController N+1 → Batch-Queries
18. **L-01** Dead Code API-Methoden entfernen
