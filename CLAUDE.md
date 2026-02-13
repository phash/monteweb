# CLAUDE.md — MonteWeb Schul-Intranet

MonteWeb: modulares, selbst-gehostetes Schul-Intranet für Montessori-Schulkomplexe (Krippe bis Oberstufe).
Räume, Feed, Direktnachrichten, Jobbörse (Elternstunden), Putz-Organisation (QR-Check-in), Kalender, Formulare, Fotobox.

**GitHub:** https://github.com/phash/monteweb (privat) | **Alle 19 Phasen COMPLETE** | Flyway V001–V041

## Tech-Stack

**Backend:** Java 21, Spring Boot 3.4.2, Spring Modulith 1.3.2, Spring Security (JWT+Redis), Spring Data JPA + Flyway, PostgreSQL 16, Redis 7, MinIO, Maven, Testcontainers
**Frontend:** Vue 3.5 (`<script setup>` + TS 5.9), Vite 7.3, PrimeVue 4.5 (Aura), Pinia 3, Vue Router 4.6, vue-i18n 11, Axios, Vitest 4
**Infra:** Docker Compose, nginx reverse proxy, GitHub Actions CI/CD, Prometheus + Grafana (optional)

## Architektur-Regeln

### Backend (Spring Modulith)

1. **Modulstruktur:** Jedes Modul = direktes Sub-Package von `com.monteweb` (NICHT unter `core/` oder `modules/`).
   - **Public API** (Root-Package): `*ModuleApi` Facades, `*Info` Records, Events, Enums
   - **Internal** (`internal/`): Service, Controller, Model, Repository — **niemals** von anderen Modulen importieren!
   - Inter-Modul: Facades (sync), Spring Events (async)

2. **Shared:** `com.monteweb.shared` = kein Modul, Querschnittsthemen via `@NamedInterface` (`shared-dto`, `shared-exception`, `shared-util`, `shared-config`).

3. **API-Design:** REST `/api/v1/`, Antworten als `ApiResponse<T>`:
   ```java
   public record ApiResponse<T>(T data, String message, boolean success) {}
   ```

4. **Optionale Module:** `@ConditionalOnProperty(prefix = "monteweb.modules", name = "xyz.enabled")` auf **ALLE** Beans, nicht nur Config.

5. **Flyway:** V001–V041. Jede Schema-Änderung als neue Migration. Hibernate `ddl-auto: validate`.

6. **Security:** JWT (15min Access + 7d Refresh). Rate-Limiting auf Auth-Endpoints. Fotobox-Bild-Endpoints: JWT auch via `?token=` Query-Parameter.

### Frontend

1. **Composition API + `<script setup lang="ts">`** — kein Options API
2. **PrimeVue 4:** Aura-Theme, einzeln importieren. `ToastService` global (`main.ts`), `<Toast />` in `App.vue`, Views: `useToast()`
3. **Pinia:** Ein Store pro Domäne, Stores rufen API-Funktionen auf
4. **i18n:** Alle UI-Texte als Keys (`de.ts` + `en.ts`). Browser-Locale-Detection. Keine hardcodierten Strings
5. **API-Client:** Zentraler Axios-Client (`api/client.ts`) mit JWT-Interceptor (auto-refresh)
6. **Routing:** Lazy-loaded, Auth-Guards, 404 Catch-all
7. **Responsive:** BottomNav mobile, Sidebar desktop
8. **Theming:** CSS Custom Properties `--mw-*`, Theme vom Backend geladen

## Module (Backend-Packages unter `com.monteweb`)

| Modul | Beschreibung | Conditional |
|-------|-------------|-------------|
| auth | JWT, Password-Reset, OIDC/SSO | OIDC: `monteweb.oidc.enabled` |
| user | Profil, Rollen, Suche, DSGVO | - |
| family | Familienverbund, Einladungen, Stundenkonto | - |
| school | Schulbereiche (Krippe–OS) | - |
| room | Räume, Diskussions-Threads, Beitrittsanfragen | - |
| feed | Unified Feed, Posts, Kommentare, Banner | - |
| notification | In-App + Push (VAPID) | Push: `monteweb.push.enabled` |
| admin | System-Config, Audit-Log, Module | - |
| messaging | DM & Chat, Kommunikationsregeln | `monteweb.modules.messaging.enabled` |
| files | Dateiablage via MinIO | `monteweb.modules.files.enabled` |
| jobboard | Jobbörse, Elternstunden, PDF-Export | `monteweb.modules.jobboard.enabled` |
| cleaning | Putz-Orga, QR-Check-in, PDF | `monteweb.modules.cleaning.enabled` |
| calendar | Events (Raum/Bereich/Schule), RSVP | `monteweb.modules.calendar.enabled` |
| forms | Survey/Consent, Scopes, CSV/PDF-Export | `monteweb.modules.forms.enabled` |
| fotobox | Foto-Threads, Thumbnails, Lightbox | `monteweb.modules.fotobox.enabled` |

Weitere conditional Features: E-Mail (`monteweb.email.enabled`), OIDC/SSO (`monteweb.oidc.enabled`), Push (`monteweb.push.enabled`)

## Wichtige fachliche Regeln

1. **Familienverbund = Abrechnungseinheit.** Stunden aus Jobbörse/Putz werden Familie gutgeschrieben (Putzstunden: Sonder-Unterkonto)
2. **Ein Elternteil = ein Familienverbund.** Kind kann mehreren zugeordnet sein (getrennte Eltern)
3. **Putz-Orga ist Opt-in**, nicht Rotation
4. **Feed-Banner:** kontextabhängig (Putz-Banner nur für betroffene Eltern)
5. **Raum-Posts = Feed-Einträge** aller Mitglieder (rollenabhängig)
6. **Module abschaltbar:** Backend via `@ConditionalOnProperty`, Frontend: Menü nur wenn Modul aktiv
7. **Kommunikationsregeln:** Lehrer↔Eltern immer erlaubt. Eltern↔Eltern / Schüler↔Schüler: konfigurierbar
8. **Kalender-Berechtigungen:** ROOM→LEADER/SUPERADMIN, SECTION→TEACHER/SUPERADMIN, SCHOOL→SUPERADMIN
9. **Raum-Beitrittsanfragen:** Non-Members anfragen, LEADER genehmigt/lehnt ab, auto-MEMBER bei Genehmigung
10. **Familien-Einladungen:** Per User-Suche mit Rollenwahl (PARENT/CHILD), Annehmen/Ablehnen via Notification
11. **Fotobox:** VIEW_ONLY < POST_IMAGES < CREATE_THREADS. LEADER/SUPERADMIN = CREATE_THREADS. MinIO, Thumbnails auto, Content-Type aus Magic Bytes, max 20 Dateien/Upload

## Konventionen

- **Code:** Englisch. **UI-Texte:** Deutsch + Englisch (i18n). **Git:** Conventional Commits
- **Java:** `com.monteweb.{modul}` + `.internal`, Records für DTOs, `*Info` Public DTOs, `*ModuleApi` Facades, Entities: Lombok, UUIDs als PK, `Instant` für Timestamps, Bean Validation auf Requests
- **Vue/TS:** `<script setup lang="ts">`, PascalCase Komponenten, `use`-Prefix Composables, Typen in `types/`, Scoped Styles, `--mw-*` CSS Props
- **API:** `/api/v1/`, Paginierung `?page=0&size=20&sort=createdAt,desc`, ISO 8601 Datumsformat

## API-Endpunkte (Kurzreferenz)

- **Auth** `/api/v1/auth`: register, login, logout, refresh, password-reset, oidc/config, oidc/token
- **Users** `/api/v1/users`: /me (GET/PUT), /me/avatar, /me/data-export, DELETE /me (DSGVO), /{id}, /search
- **Admin Users** `/api/v1/admin/users`: CRUD, Rollen, Status, Familien, Sonderrollen
- **Families** `/api/v1/families`: CRUD, /mine, /join, invite, children, hours, invitations (send/accept/decline)
- **Sections** `/api/v1/sections`: CRUD
- **Rooms** `/api/v1/rooms`: /mine, /browse, /discover, CRUD, settings, avatar, archive, join/leave, members, mute, join-requests (send/approve/deny)
- **Room Chat** `/api/v1/rooms/{id}/chat`: channels
- **Threads** `/api/v1/rooms/{id}/threads`: CRUD (LEADER), replies (members), archive
- **Feed** `/api/v1/feed`: feed, banners, posts CRUD, pin, comments
- **Messaging** `/api/v1/messages`: conversations, messages, WS `/ws/messages`
- **Files** `/api/v1/rooms/{id}/files`: upload/download/delete, folders
- **Jobboard** `/api/v1/jobs`: CRUD, apply, assignments (start/complete/confirm), family hours, report/export/pdf
- **Cleaning** `/api/v1/cleaning`: slots, register, swap, checkin/checkout, configs, generate, qr-codes, dashboard
- **Calendar** `/api/v1/calendar`: events CRUD, cancel, rsvp, room events
- **Forms** `/api/v1/forms`: CRUD, publish, close, respond, results, responses, csv/pdf export
- **Fotobox** `/api/v1/rooms/{id}/fotobox` + `/api/v1/fotobox`: settings, threads, images (upload/download/thumbnail with `?token=` JWT)
- **Notifications** `/api/v1/notifications`: list, unread-count, read, read-all, WS, push (subscribe/unsubscribe/public-key)
- **Admin** `/api/v1/admin`: config, theme, modules, logo, audit-log
- **Actuator**: /health, /info, /prometheus, /metrics

## Entwicklung

**Voraussetzung:** Docker + Node.js 22+. Java NICHT lokal nötig.

```bash
# Full stack (Production)
docker compose up -d                    # http://localhost (Port 80)
docker compose build && docker compose up -d  # Nach Code-Änderungen

# Frontend dev (hot reload) — Backend muss via Docker laufen
cd frontend && npm install && npm run dev   # http://localhost:5173

# Tests
cd frontend && npm test                 # 868 Tests, 106 Dateien
cd backend && mvn test                  # 37 Testdateien, Testcontainers

# Monitoring (optional)
docker compose --profile monitoring up -d  # Grafana :3000, Prometheus :9090
```

**Dev-Ports:** PostgreSQL 5433, Redis 6380, MinIO 9000/9001, Backend 8090, Frontend 5173 (dev) / 8091 (Docker)

## Flyway-Migrationen (V001–V041)

V001 tenant_config | V002 users | V003 sections | V004 families | V005 rooms | V006 audit_log | V007 seed_tenant | V008 feed | V009 notifications | V010 conversations | V011 files | V012 event_publication | V013 jobs | V014 assignments | V015 cleaning_configs | V016 cleaning_slots | V017 interest_fields | V018 chat_channels | V019 DSGVO | V020 password_reset | V021 comm_rules | V022 discussions | V023 push_subs | V024 OIDC | V025 calendar | V026 calendar_default | V027 job_event_link | V028 forms | V029 forms_default | V030 target_cleaning_hours | V031 avatars | V032 seed_admin | V033 seed_test_users | V034 join_requests | V035 family_invitations | V036 thread_audience | V037 role_refactoring | V038 fotobox | V039 fotobox_fix | V040 seed_realistic | V041 feedback_batch_1

## DB-Hinweise

- `room_members`: Composite PK (room_id, user_id) — kein `id`
- `rooms.is_archived` (nicht `archived`)
