# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MonteWeb: modulares, selbst-gehostetes Schul-Intranet fuer Montessori-Schulkomplexe (Krippe bis Oberstufe).
Raeume, Feed, Direktnachrichten, Jobboerse (Elternstunden), Putz-Organisation (QR-Check-in), Kalender, Formulare, Fotobox.

**Tech:** Java 21 + Spring Boot 3.4 + Spring Modulith 1.3 | Vue 3.5 + TS 5.9 + PrimeVue 4 Aura | PostgreSQL 16, Redis 7, MinIO | Docker Compose + nginx

## Commands

```bash
# Full stack (Docker) — http://localhost (port 80)
docker compose up -d
docker compose build && docker compose up -d          # rebuild all
docker compose build backend && docker compose up backend -d   # backend only
docker compose build frontend && docker compose up frontend -d # frontend only

# Dev infrastructure only (postgres:5433, redis:6380, minio:9000/9001)
docker compose -f docker-compose.dev.yml up -d

# Frontend dev (hot reload, proxies /api to localhost:8080) — needs backend via Docker
cd frontend && npm install && npm run dev              # http://localhost:5173
npm run build          # vue-tsc + vite build
npm test               # vitest run (891 tests, 107 files)
npm run test:watch     # vitest watch mode
npm run test:coverage

# Backend tests (requires Docker for Testcontainers)
cd backend
./mvnw test                                            # all tests
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
├── composables/   # useLocaleDate, useWebSocket, useTheme, usePushNotifications, useHolidays
├── i18n/          # de.ts + en.ts — ALL user-facing text via t(), German default
├── router/        # Lazy-loaded routes, auth/admin guards, 404 catch-all
├── stores/        # Pinia composition stores (one per domain)
├── types/         # TypeScript interfaces mirroring backend DTOs
└── views/         # Page components, views/admin/ for admin pages
```

**Data flow:** View → Pinia store action → API module → shared axios client (`api/client.ts`, auto JWT, token refresh interceptor) → `ApiResponse<T>` response.

**PrimeVue:** `ToastService` registered globally in `main.ts`, `<Toast />` in `App.vue`, views use `useToast()`. Components imported individually.

**Theming:** CSS custom properties `--mw-*`, theme loaded from backend tenant config.

### Database

- **Flyway** V001–V044. Never modify existing migrations — always create new `V0XX__description.sql`. Hibernate `ddl-auto: validate`
- UUID PKs (`DEFAULT gen_random_uuid()`), `TIMESTAMP WITH TIME ZONE`, PostgreSQL arrays (`TEXT[]`, `UUID[]`), JSONB
- `room_members`: composite PK `(room_id, user_id)` — no `id` column
- `rooms.is_archived` (not `archived`)
- `feed_posts.target_user_ids`: `UUID[]` — NULL=visible to all, filled=only listed users
- `cleaning_configs.specific_date`: optional DATE for one-time Putzaktionen
- `tenant_config.bundesland`: VARCHAR(5) default `'BY'`, determines public holidays
- `tenant_config.school_vacations`: JSONB array of `{name, from, to}`

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
| feed | Unified Feed, Posts, Kommentare, Banner, Targeted Posts | - |
| notification | In-App + Push (VAPID) | Push: `monteweb.push.enabled` |
| admin | System-Config, Audit-Log, Module | - |
| messaging | DM & Chat, Kommunikationsregeln | `monteweb.modules.messaging.enabled` |
| files | Dateiablage via MinIO | `monteweb.modules.files.enabled` |
| jobboard | Jobboerse, Elternstunden, PDF-Export | `monteweb.modules.jobboard.enabled` |
| cleaning | Putz-Orga, QR-Check-in, PDF, Putzaktionen | `monteweb.modules.cleaning.enabled` |
| calendar | Events (Raum/Bereich/Schule), RSVP, Cancel→Feed | `monteweb.modules.calendar.enabled` |
| forms | Survey/Consent, Scopes, CSV/PDF-Export | `monteweb.modules.forms.enabled` |
| fotobox | Foto-Threads, Thumbnails, Lightbox | `monteweb.modules.fotobox.enabled` |

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

## Conventions

- **Code:** English. **UI-Texte:** German + English (i18n). **Git:** Conventional Commits
- **Java:** Records for DTOs, `*Info` public DTOs, `*ModuleApi` facades, Lombok entities, UUIDs as PK, `Instant` for timestamps, Bean Validation on requests
- **Vue/TS:** `<script setup lang="ts">`, `@/` path alias, PascalCase components, `use`-prefix composables, types in `types/`, scoped styles
- **API:** `/api/v1/`, `ResponseEntity<ApiResponse<T>>`, `SecurityUtils.requireCurrentUserId()` in controllers, pagination `?page=0&size=20&sort=createdAt,desc`

## API Endpoints (Quick Reference)

- **Auth** `/api/v1/auth`: register, login, logout, refresh, password-reset, oidc/config, oidc/token
- **Users** `/api/v1/users`: /me (GET/PUT), /me/avatar, /me/data-export, DELETE /me (DSGVO), /{id}, /search
- **Admin** `/api/v1/admin/users`: CRUD, roles, status | `/api/v1/admin`: config, theme, modules, logo, audit-log
- **Families** `/api/v1/families`: CRUD, /mine, /join, invite, children, hours, invitations
- **Rooms** `/api/v1/rooms`: /mine, /browse, /discover, CRUD, settings, avatar, archive, members, mute, join-requests
- **Feed** `/api/v1/feed`: feed, banners, posts CRUD, pin, comments
- **Calendar** `/api/v1/calendar`: events CRUD, cancel, rsvp, room events
- **Messaging** `/api/v1/messages`: conversations, messages, WS `/ws/messages`
- **Files** `/api/v1/rooms/{id}/files`: upload/download/delete, folders
- **Jobboard** `/api/v1/jobs`: CRUD, apply, assignments, family hours, report/export/pdf
- **Cleaning** `/api/v1/cleaning`: slots, register, swap, checkin/checkout, configs, generate, qr-codes, dashboard
- **Forms** `/api/v1/forms`: CRUD, publish, close, respond, results, csv/pdf export
- **Fotobox** `/api/v1/rooms/{id}/fotobox` + `/api/v1/fotobox`: threads, images, thumbnails (`?token=` JWT)
- **Notifications** `/api/v1/notifications`: list, unread-count, read, read-all, push subscribe/unsubscribe
