# MonteWeb Comprehensive Cleanup — Design

**Datum:** 2026-03-02
**Ziel:** Alle offenen Audit-Items abarbeiten, kaputte Tests fixen, Code-Smells bereinigen
**Ansatz:** 5 parallele Agents mit strikter Dateiscope-Trennung

---

## Scope

13 offene Audit-Items (M-06 bis M-10, L-01 bis L-04, L-08 bis L-11) + 18 kaputte UserProfileView-Tests + weitere Code-Smells.

## Agent-Aufteilung

### Agent A — Backend: Controller → Service Extraktion
- **M-08:** Jobboard Attachment-Logik (upload/download/delete) aus Controller in `JobboardService` verschieben
- **M-09:** Privacy Consent-Logik aus Controller in neuen `PrivacyService` extrahieren (inkl. IDOR-Check)
- **M-10:** RoomController `buildDetailResponse()` N+1 fixen — Batch-Queries für Member-Infos
- **Scope:** `backend/src/main/java/com/monteweb/{jobboard,user,room}/internal/`

### Agent B — Frontend: Store Error-Handling + Loading Pattern
- **M-06:** Standardisiertes Error-Handling für ~67 Store-Actions — try/catch + Toast
- **M-07:** Shared `loading` Refs auftrennen in `rooms.ts`, `cleaning.ts`, `jobboard.ts`, `calendar.ts`
- **L-02:** Dead State `currentPost` aus Feed Store entfernen
- **L-11:** `as any` Cast in rooms.ts ersetzen
- **Scope:** `frontend/src/stores/`

### Agent C — Frontend: Dead Code + API Cleanup
- **L-01:** 23 ungenutzte API-Methoden entfernen (mit Grep-Verifikation)
- **L-03:** `cleaning.api.ts` auf Object-Export-Pattern umstellen
- **L-04:** API-Bypasses nach API-Module verschieben (außer `useErrorReporting.ts` — intentional)
- **Scope:** `frontend/src/api/`, `frontend/src/views/LoginView.vue`, `frontend/src/composables/usePushNotifications.ts`

### Agent D — Backend: DB + Scheduler
- **L-08:** Index auf `cleaning_configs.room_id` (V109 Migration)
- **L-09:** `@Scheduled` Cleanup für expired `password_reset_tokens`
- **Scope:** `backend/src/main/resources/db/migration/`, `backend/src/main/java/com/monteweb/auth/internal/`

### Agent E — Tests: UserProfileView fixen
- 18 Failures in `UserProfileView.test.ts` fixen (Impersonation-Mocks fehlen)
- Weitere kaputte Tests finden und fixen
- **Scope:** `frontend/src/**/__tests__/`

## Keine Datei-Konflikte

Jeder Agent hat einen strikten, nicht-überlappenden Dateiscope. Keine parallelen Schreibzugriffe auf dieselben Dateien.

## Erfolgskriterien

1. `vue-tsc --noEmit` — clean (0 errors)
2. `npx vitest run` — 0 failures (aktuell 18)
3. `docker compose build backend` — BUILD SUCCESS
4. Audit-Report: alle Items auf FIXED oder begründet WONTFIX

## L-10 (Migration-Nummerierungslücke V079)

Bewusst WONTFIX — kosmetisch, kein funktionales Problem, rückwirkende Änderung an Flyway-Nummerierung ist riskant.
