# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MonteWeb: modulares, selbst-gehostetes Schul-Intranet fuer Montessori-Schulkomplexe (Krippe bis Oberstufe).
Raeume, Feed, Direktnachrichten, Jobboerse (Elternstunden), Putz-Organisation (QR-Check-in), Kalender, Formulare, Fotobox.

**Tech:** Java 21 + Spring Boot 3.4 + Spring Modulith 1.3 | Vue 3.5 + TS 5.9 + PrimeVue 4 Aura | PostgreSQL 16, Redis 7, MinIO, Solr 9.8 | Docker Compose + Caddy (SSL) + nginx

**20 backend modules**, 96 Flyway migrations (V001–V097), ~1341 frontend tests, ~490 backend tests

## Commands

```bash
# Full stack (Docker) — http://localhost (port 80/443)
# Set DOMAIN in .env: localhost (no SSL) or monteweb.deineschule.de (auto SSL)
docker compose up -d
docker compose build && docker compose up -d          # rebuild all
docker compose build backend && docker compose up backend -d   # backend only
docker compose build frontend && docker compose up frontend -d # frontend only

# Dev infrastructure only (postgres:5433, redis:6380, minio:9000/9001)
docker compose -f docker-compose.dev.yml up -d

# Frontend dev (hot reload, proxies /api to localhost:8080) — needs backend via Docker
cd frontend && npm install && npm run dev              # http://localhost:5173
npm run build          # vue-tsc + vite build
npm test               # vitest run (~1341 tests, ~147 files)
npm run test:watch     # vitest watch mode
npm run test:coverage

# Backend tests (requires Docker for Testcontainers)
cd backend
./mvnw test                                            # all tests (47 test classes, ~490 tests)
./mvnw test -Dtest=AuthControllerIntegrationTest       # single test class
./mvnw test -Dtest="AuthControllerIntegrationTest#register_*"  # single method

# Monitoring (optional)
docker compose --profile monitoring up -d              # Grafana :3000, Prometheus :9090
```

**Test Accounts** (password: `test1234`): `admin@monteweb.local` (SUPERADMIN), `lehrer@monteweb.local` (TEACHER), `eltern@monteweb.local` (PARENT), `schueler@monteweb.local` (STUDENT), `sectionadmin@monteweb.local` (SECTION_ADMIN). Plus ~220 realistic seed users from V040.

## Architecture

### Backend (Spring Modulith)

Package `com.monteweb`. Each module = direct sub-package (NOT under `core/` or `modules/`):

```
com.monteweb.<module>/
├── <Module>ModuleApi.java      # Public facade interface — ONLY cross-module access point
├── <Module>Info.java           # Public DTO record for cross-module data
├── <Module>Event.java          # Public event records (async cross-module)
└── internal/
    ├── config/                 # @ConditionalOnProperty for optional modules
    ├── controller/             # REST controllers (/api/v1/...)
    ├── dto/                    # Request/response DTOs (module-internal)
    ├── model/                  # JPA entities (Lombok, UUID PKs, Instant timestamps)
    ├── repository/             # Spring Data JPA
    └── service/                # Implements *ModuleApi facade
```

**Critical rules:**
- **NEVER** import from another module's `internal/` package. Use `*ModuleApi` facades (sync) or Spring `ApplicationEventPublisher` (async)
- **Shared** (`com.monteweb.shared`): not a module — cross-cutting via `@NamedInterface` (`shared-dto`, `shared-exception`, `shared-util`, `shared-config`). Provides `ApiResponse<T>`, `PageResponse<T>`, `SecurityUtils`, exception hierarchy, `PdfService`
- **Optional modules:** `@ConditionalOnProperty(prefix = "monteweb.modules", name = "xyz.enabled")` on **ALL** beans (not just Config). Use `@Autowired(required = false)` for optional injection
- **Security:** JWT (15min access + 7d refresh), rate-limiting on auth endpoints. Fotobox image endpoints accept JWT via `?token=` query parameter

### Frontend (Vue 3)

```
frontend/src/
├── api/           # Axios modules (authApi, feedApi, roomsApi...) — base /api/v1, auto JWT refresh
├── components/    # By domain: common/, layout/, feed/, rooms/, family/, messaging/
├── composables/   # useLocaleDate, useWebSocket, useTheme, usePushNotifications, useHolidays, useConfirmDialog, useErrorReporting, useContextHelp, usePwaInstall
├── i18n/          # de.ts + en.ts — ALL user-facing text via t(), German default
├── router/        # Lazy-loaded routes, auth/admin guards, 404 catch-all
├── stores/        # Pinia composition stores (one per domain)
├── types/         # TypeScript interfaces mirroring backend DTOs
└── views/         # Page components, views/admin/ for admin pages
```

**Data flow:** View → Pinia store action → API module → shared axios client (`api/client.ts`, auto JWT, token refresh interceptor) → `ApiResponse<T>` response.

**PrimeVue:** `ToastService` registered globally in `main.ts`, `<Toast />` in `App.vue`, views use `useToast()`. Components imported individually.

**Theming:** CSS custom properties `--mw-*`, theme loaded from backend tenant config.

**PWA:** Installable via `vite-plugin-pwa` + Workbox. Service worker with runtime caching (NetworkFirst for API calls). Icons in `public/icons/`. `usePwaInstall` composable handles install prompt with 7-day dismiss delay.

### Database

- **Flyway** V001–V097 (96 migrations). Never modify existing migrations — always create new `V0XX__description.sql`. Hibernate `ddl-auto: validate`
- UUID PKs (`DEFAULT gen_random_uuid()`), `TIMESTAMP WITH TIME ZONE`, PostgreSQL arrays (`TEXT[]`, `UUID[]`), JSONB
- `room_members`: composite PK `(room_id, user_id)` — no `id` column
- `rooms.is_archived` (not `archived`)
- `feed_posts.target_user_ids`: `UUID[]` — NULL=visible to all, filled=only listed users
- `cleaning_configs.specific_date`: optional DATE for one-time Putzaktionen
- `tenant_config.bundesland`: VARCHAR(5) default `'BY'`, determines public holidays
- `tenant_config.school_vacations`: JSONB array of `{name, from, to}`
- `tenant_config.github_repo`: VARCHAR — GitHub repo for error report issue creation
- `tenant_config.github_pat`: VARCHAR — GitHub Personal Access Token for issue creation
- `room_folders.audience`: VARCHAR(20) default `'ALL'` — visibility: ALL, PARENTS_ONLY, STUDENTS_ONLY
- `fotobox_threads.audience`: VARCHAR(20) default `'ALL'` — same visibility as folders
- `forms.section_ids`: `UUID[]` with GIN index — multi-section targeting for SECTION-scoped forms
- `billing_periods`: family billing with year/month/status (OPEN/CLOSED) — Jahresabrechnung
- `error_reports`: fingerprint-based dedup, status (NEW/REPORTED/RESOLVED/IGNORED), `github_issue_url`, occurrence tracking
- `fundgrube_items`: lost & found items with section filter, claim workflow (expires +24h)
- `fundgrube_images`: MinIO image storage with thumbnails for lost & found
- `messages.reply_to_id`: UUID FK for reply threading (ON DELETE SET NULL)
- `messages.content`: nullable (image-only messages)
- `message_images`: chat images with MinIO storage, thumbnails, 90-day auto-cleanup
- `cleaning_configs.calendar_event_id` + `cleaning_configs.job_id`: links Putzaktion to calendar event and job
- `families.is_hours_exempt`: BOOLEAN default false, exempts family from Elternstunden
- `families.is_active`: BOOLEAN default true, family deactivation support
- `tenant_config.require_assignment_confirmation`: BOOLEAN default true, auto-confirms job hours when false
- `tenant_config.available_languages`: TEXT[] default `'{de,en}'`, stores selectable languages
- `tenant_config.modules`: JSONB map of all feature toggles. Core modules (messaging, files, jobboard, cleaning, calendar, forms, fotobox, fundgrube, bookmarks, tasks, wiki, profilefields) plus DB-managed toggles migrated from dedicated columns (jitsi, wopi, clamav, maintenance, ldap, directoryAdminOnly). Admin UI at `/admin/modules`, backend check via `adminModuleApi.isModuleEnabled("name")`
- `feed_post_attachments`: id, post_id (FK), file_name, file_url (MinIO path), file_type, file_size, sort_order, created_at
- `task_boards` + `task_columns` + `tasks`: per-room kanban (V076), boards unique per room, columns ordered by position
- `wiki_pages` + `wiki_page_versions`: per-room wiki (V077), slug unique per room, self-referencing parent_id
- `bookmarks`: user bookmarks for posts, events, jobs, wiki pages (type + target_id)
- `profile_field_definitions` + `profile_field_values`: custom profile fields defined by admins, values per user

### Infrastructure (Docker / CI/CD)

- **Docker:** Multi-stage builds, `.dockerignore` for minimal build context, non-root containers (nginx user, monteweb user), OCI labels
- **nginx:** Security headers (X-Frame-Options, X-Content-Type-Options, Referrer-Policy, Permissions-Policy), `server_tokens off`, `client_max_body_size 50m`, actuator blocking (only `/health` public)
- **Docker Compose:** Two isolated networks (`monteweb-frontend`, `monteweb-backend`), memory limits on all services, MinIO version pinned, Solr 9.8-slim for full-text search
- **CI/CD:** GitHub Actions with SHA-pinned actions, concurrency groups, job timeouts, Docker Buildx with GHA cache, Trivy image scanning
- **Dependabot:** Weekly updates for GitHub Actions, npm, Maven, Docker base images
- **See also:** `INFRA-CHANGELOG.md` (all optimizations), `LOCAL-DEV-GUIDE.md` (comprehensive dev guide)

### Testing

**Backend:** `@SpringBootTest @AutoConfigureMockMvc @Import(TestContainerConfig.class)` — Testcontainers spins up Postgres + Redis. `MonteWebModularityTests` verifies no illegal cross-module dependencies. JaCoCo 70% instruction minimum.

**Frontend:** Vitest + jsdom + @vue/test-utils. Setup mocks `localStorage` and PrimeVue `useToast`. Pattern: `vi.mock()` API modules, `setActivePinia(createPinia())` in `beforeEach`. 55% statement coverage threshold.

## Modules

| Modul | Beschreibung | Conditional |
|-------|-------------|-------------|
| auth | JWT, Password-Reset, OIDC/SSO | OIDC: `monteweb.oidc.enabled` |
| user | Profil, Rollen, Suche, DSGVO | - |
| family | Familienverbund, Einladungen, Stundenkonto | - |
| school | Schulbereiche (Krippe–OS) | - |
| room | Raeume, Diskussions-Threads, Beitrittsanfragen | - |
| feed | Unified Feed, Posts, Kommentare, Banner, Targeted Posts, Datei-Anhaenge | - |
| notification | In-App + Push (VAPID) | Push: `monteweb.push.enabled` |
| admin | System-Config, Audit-Log, Module, Error Reporting | - |
| messaging | DM & Chat, Kommunikationsregeln, Chat-Bilder, Antworten, Chat-Stummschaltung | `monteweb.modules.messaging.enabled` |
| files | Dateiablage via MinIO, Folder-Audience | `monteweb.modules.files.enabled` |
| jobboard | Jobboerse, Elternstunden, Jahresabrechnung, PDF-Export | `monteweb.modules.jobboard.enabled` |
| cleaning | Putz-Orga, QR-Check-in, PDF, Putzaktionen | `monteweb.modules.cleaning.enabled` |
| calendar | Events (Raum/Bereich/Schule), RSVP, Cancel→Feed | `monteweb.modules.calendar.enabled` |
| forms | Survey/Consent, Multi-Section Scopes, Dashboard Widget, CSV/PDF-Export | `monteweb.modules.forms.enabled` |
| fotobox | Foto-Threads, Thumbnails, Lightbox, Thread-Audience | `monteweb.modules.fotobox.enabled` |
| fundgrube | Schulweite Fundgrube, Fotos, Bereichsfilter, Claim-Workflow | `monteweb.modules.fundgrube.enabled` |
| bookmarks | Lesezeichen fuer Posts, Events, Jobs, Wiki-Seiten | `monteweb.modules.bookmarks.enabled` |
| tasks | Kanban-Board pro Raum, Aufgaben, Spalten | `monteweb.modules.tasks.enabled` |
| wiki | Wiki pro Raum, Markdown, Versionen, Hierarchie | `monteweb.modules.wiki.enabled` |
| profilefields | Benutzerdefinierte Profilfelder | `monteweb.modules.profilefields.enabled` |
| search | Globale Suche (Ctrl+K), Solr Volltextsuche mit Tika-Extraktion | Solr: `monteweb.modules.solr.enabled` |

**DB-managed toggles** (in `tenant_config.modules` JSONB, toggled via Admin UI):

| Toggle | Beschreibung |
|--------|-------------|
| jitsi | Jitsi-Videokonferenzen in Kalender-Events und Raum-Chats |
| wopi | ONLYOFFICE-Integration: Dokumente im Browser bearbeiten |
| clamav | ClamAV-Virenscanner fuer Datei-Uploads |
| maintenance | Wartungsmodus: System fuer nicht-Admins sperren |
| ldap | LDAP/Active Directory Authentifizierung |
| directoryAdminOnly | Benutzerverzeichnis nur fuer Admins sichtbar |

Additional toggles: E-Mail (`monteweb.email.enabled`), OIDC/SSO (`monteweb.oidc.enabled`), Push (`monteweb.push.enabled`)

## Business Rules

1. **Familienverbund = Abrechnungseinheit.** Stunden aus Jobboerse/Putz werden Familie gutgeschrieben (Putzstunden: Sonder-Unterkonto)
2. **Ein Elternteil = ein Familienverbund.** Kind kann mehreren zugeordnet sein (getrennte Eltern)
3. **Putz-Orga ist Opt-in**, nicht Rotation. Einmalig (mit Datum) oder wiederkehrend (Wochentag). DatePicker zeigt Feiertage (rot) und Schulferien (orange) je Bundesland
4. **Feed-Banner:** kontextabhaengig (Putz-Banner nur fuer betroffene Eltern)
5. **Module abschaltbar:** Backend via `@ConditionalOnProperty`, Frontend: Menue nur wenn Modul aktiv
6. **Kommunikationsregeln:** Lehrer↔Eltern immer erlaubt. Eltern↔Eltern / Schueler↔Schueler: konfigurierbar
7. **Kalender-Berechtigungen:** ROOM→LEADER/SUPERADMIN, SECTION→TEACHER/SUPERADMIN, SCHOOL→SUPERADMIN. Absage→Feed fuer alle, Loeschung→Feed nur fuer Zusager
8. **Raum-Beitrittsanfragen:** Non-Members anfragen, LEADER genehmigt/lehnt ab, auto-MEMBER bei Genehmigung
9. **Familien-Einladungen:** Per User-Suche mit Rollenwahl (PARENT/CHILD), Annehmen/Ablehnen via Notification
10. **Fotobox:** VIEW_ONLY < POST_IMAGES < CREATE_THREADS. LEADER/SUPERADMIN = CREATE_THREADS. MinIO, Thumbnails auto, Content-Type aus Magic Bytes, max 20 Dateien/Upload
11. **Targeted Feed Posts:** `feed_posts.target_user_ids UUID[]` — NULL=fuer alle sichtbar, gefuellt=nur fuer diese User
12. **Audience-Sichtbarkeit:** Ordner und Fotobox-Threads haben `audience` (ALL, PARENTS_ONLY, STUDENTS_ONLY). Parents erstellen automatisch PARENTS_ONLY; Teachers/Leaders/Admins waehlen
13. **Multi-Section Forms:** SECTION-scoped Formulare koennen mehrere Schulbereiche via `section_ids UUID[]` targeten. Dashboard-Widget zeigt offene Formulare
14. **Auto-Folder Creation:** When a KLASSE room is created (`RoomCreatedEvent`), the files module automatically creates a default folder for the room
15. **Error Reporting:** Frontend errors are reported via `/api/v1/error-reports` with fingerprint-based deduplication. Admin can view, manage status (NEW/REPORTED/RESOLVED/IGNORED), and optionally create GitHub Issues via configured `github_repo` + `github_pat`
16. **Fundgrube (Lost & Found):** Schulweite Fundgrube mit Fotos, optionalem Bereichsfilter. Claim-Workflow (expires +24h via `@Scheduled` cleanup). MinIO image storage mit Thumbnails
17. **Chat-Bilder & Antworten:** Nachrichten koennen Bilder enthalten (multipart upload, MinIO, Thumbnails). Reply-Threading via `reply_to_id`. 90-Tage Auto-Cleanup fuer Bilder
18. **PWA:** Installierbar als Progressive Web App. Workbox Service Worker mit NetworkFirst-Caching fuer API-Calls. Install-Banner mit 7-Tage-Dismiss
19. **Mehrsprachigkeit:** `available_languages TEXT[]` bestimmt waehlbare Sprachen. LanguageSwitcher in Profil + Login (nicht Header). Nur sichtbar wenn >1 Sprache aktiviert
20. **Familien-Deaktivierung:** Familien koennen deaktiviert werden (`is_active`). Stundenkonto-Befreiung via `is_hours_exempt`
21. **Chat-Stummschaltung:** Conversations koennen stummgeschaltet werden (`conversation_participants.muted`). Mute-Toggle in DM-View und RoomChat-Header. Profilseite zeigt alle stummgeschalteten Chats mit Unmute-Buttons
22. **Feed-Anhaenge:** Posts koennen Datei-Anhaenge haben (MinIO upload, multi-file). Zwei-Schritt: Post erstellen → Dateien hochladen
23. **Solr-Volltextsuche:** Apache Solr 9.8 mit deutscher Sprachanalyse (Stemming, Stopwords). 8 Dokumenttypen (USER, ROOM, POST, EVENT, FILE, WIKI, TASK). Echtzeit-Indexierung via Spring Events. Tika-Extraktion fuer Dateiinhalte (PDF, DOCX, etc.). Admin-Reindex via `POST /api/v1/admin/search/reindex`. Fallback auf DB-Suche wenn Solr deaktiviert

## Conventions

- **Code:** English. **UI-Texte:** German + English (i18n). **Git:** Conventional Commits
- **Java:** Records for DTOs, `*Info` public DTOs, `*ModuleApi` facades, Lombok entities, UUIDs as PK, `Instant` for timestamps, Bean Validation on requests
- **Vue/TS:** `<script setup lang="ts">`, `@/` path alias, PascalCase components, `use`-prefix composables, types in `types/`, scoped styles
- **API:** `/api/v1/`, `ResponseEntity<ApiResponse<T>>`, `SecurityUtils.requireCurrentUserId()` in controllers, pagination `?page=0&size=20&sort=createdAt,desc`

## API Endpoints (Quick Reference)

- **Auth** `/api/v1/auth`: register, login, logout, refresh, password-reset, oidc/config, oidc/token
- **Users** `/api/v1/users`: /me (GET/PUT), /me/avatar, /me/data-export, DELETE /me (DSGVO), /{id}, /search
- **Admin** `/api/v1/admin/users`: CRUD, roles, status | `/api/v1/admin`: config, theme, modules, logo, audit-log, error-reports
- **Families** `/api/v1/families`: CRUD, /mine, /join, invite, children, hours, invitations
- **Rooms** `/api/v1/rooms`: /mine, /browse, /discover, CRUD, settings, avatar, archive, members, mute, join-requests
- **Feed** `/api/v1/feed`: feed, banners, posts CRUD, pin, comments, attachments (upload/download/delete)
- **Calendar** `/api/v1/calendar`: events CRUD, cancel, rsvp, room events
- **Messaging** `/api/v1/messages`: conversations, messages (multipart with images), reply threading, image download/thumbnail, WS `/ws/messages`
- **Files** `/api/v1/rooms/{id}/files`: upload/download/delete, folders
- **Billing** `/api/v1/billing`: periods, report (Jahresabrechnung)
- **Jobboard** `/api/v1/jobs`: CRUD, apply, assignments, family hours, report/export/pdf
- **Cleaning** `/api/v1/cleaning`: slots, register, swap, checkin/checkout, configs, generate, qr-codes, dashboard
- **Forms** `/api/v1/forms`: CRUD, publish, close, respond, results, csv/pdf export
- **Fotobox** `/api/v1/rooms/{id}/fotobox` + `/api/v1/fotobox`: threads, images, thumbnails (`?token=` JWT)
- **Error Reports** `/api/v1/error-reports`: submit (public) | `/api/v1/admin/error-reports`: list, update status
- **Section Admin** `/api/v1/section-admin`: rooms, members, overview for SECTION_ADMIN role
- **Fundgrube** `/api/v1/fundgrube`: items CRUD, claim, images upload/download/thumbnail (`?token=` JWT)
- **Bookmarks** `/api/v1/bookmarks`: CRUD bookmarks for posts, events, jobs, wiki pages
- **Tasks** `/api/v1/rooms/{id}/tasks`: kanban boards, columns, tasks CRUD, drag & drop reorder
- **Wiki** `/api/v1/rooms/{id}/wiki`: pages CRUD, versions, hierarchy, search
- **Profile Fields** `/api/v1/profile-fields`: list, /me (GET/PUT) | `/api/v1/admin/profile-fields`: CRUD field definitions
- **Search** `/api/v1/search`: global search (q, type, limit) | `/api/v1/admin/search`: reindex (Solr)
- **Notifications** `/api/v1/notifications`: list, unread-count, read, read-all, delete, push subscribe/unsubscribe
