# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MonteWeb: modulares, selbst-gehostetes Schul-Intranet fuer Montessori-Schulkomplexe (Krippe bis Oberstufe).
Raeume, Feed, Direktnachrichten, Jobboerse (Elternstunden), Putz-Organisation (QR-Check-in), Kalender, Formulare, Fotobox.

**Tech:** Java 21 + Spring Boot 3.4 + Spring Modulith 1.3 | Vue 3.5 + TS 5.9 + PrimeVue 4 Aura | PostgreSQL 16, Redis 7, MinIO, Solr 9.8 | Docker Compose + Caddy (SSL) + nginx

**20 backend modules**, 114 Flyway migrations (V001–V115), ~1990 frontend tests (56% coverage), ~490 backend tests, 550 Playwright E2E tests (22 test files)

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
npm test               # vitest run (~1990 tests, ~183 files)
npm run test:watch     # vitest watch mode
npm run test:coverage

# Backend tests (requires Docker for Testcontainers)
cd backend
./mvnw test                                            # all tests (47 test classes, ~490 tests)
./mvnw test -Dtest=AuthControllerIntegrationTest       # single test class
./mvnw test -Dtest="AuthControllerIntegrationTest#register_*"  # single method

# Backup (optional)
docker compose --profile backup up -d                  # automated daily backups
docker compose exec backup backup.sh                   # manual backup
docker compose exec backup restore.sh --list           # list backups
docker compose exec backup restore.sh latest           # restore latest

# Monitoring (optional)
docker compose --profile monitoring up -d              # Grafana :3000, Prometheus :9090

# Deployment (production)
./scripts/deploy.sh                  # build + deploy all
./scripts/deploy.sh --new-tunnel     # deploy + new Cloudflare Quick Tunnel
./scripts/deploy.sh --backend-only   # rebuild + restart backend only
./scripts/deploy.sh --frontend-only  # rebuild + restart frontend only
./scripts/deploy.sh --status         # show service status + tunnel URL
```

**Test Accounts:** `admin@monteweb.local` / `admin123` (SUPERADMIN, V032), `lehrer@monteweb.local` / `test1234` (TEACHER), `eltern@monteweb.local` / `test1234` (PARENT), `schueler@monteweb.local` / `test1234` (STUDENT), `sectionadmin@monteweb.local` / `test1234` (SECTION_ADMIN). Plus ~220 seed users from V040 (all `test1234`). **Note:** On fresh prod deployments, named test accounts may need manual creation (see `docs/DEPLOYMENT.md`).

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
- **Security:** JWT (15min access + 7d refresh), rate-limiting on auth endpoints. Fotobox image endpoints accept JWT via `?token=` query parameter. TOTP secrets encrypted at rest (AES-256-GCM via `AesEncryptionService`). CSV-imported users get random passwords + `forcePasswordChange` flag
- **User Deletion (DSGVO Art. 17):** `UserDeletionExecutedEvent` triggers DeletionListeners in ALL 15 data-holding modules (feed, room, family, messaging, jobboard, cleaning, calendar, forms, fotobox, fundgrube, files, bookmarks, tasks, wiki, profilefields, notification). Each listener calls `service.cleanupUserData(userId)` — either deletes or anonymizes data depending on ownership

### Frontend (Vue 3)

```
frontend/src/
├── api/           # Axios modules (authApi, feedApi, roomsApi...) — base /api/v1, auto JWT refresh
├── components/    # By domain: common/, layout/, feed/, rooms/, family/, messaging/
├── composables/   # useLocaleDate, useWebSocket, useTheme, useDarkMode, usePushNotifications, useHolidays, useConfirmDialog, useErrorReporting, useContextHelp, usePwaInstall, useMentions, useImageToken
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

- **Flyway** V001–V115 (114 migrations). Never modify existing migrations — always create new `VXXX__description.sql`. Hibernate `ddl-auto: validate`
- UUID PKs, `TIMESTAMP WITH TIME ZONE`, PostgreSQL arrays, JSONB
- **Key gotchas:** `room_members` has composite PK (no `id`), `rooms.is_archived` (NOT `archived`), `messages.content` is nullable
- **See:** [`docs/DATABASE-SCHEMA.md`](docs/DATABASE-SCHEMA.md) for full schema reference

### Infrastructure (Docker / CI/CD)

- **Docker Compose:** 6 core services (postgres, redis, minio, solr, backend, frontend). Two isolated networks. Memory limits on all services. Optional profiles: `ssl` (Caddy), `monitoring` (Prometheus+Grafana), `office` (OnlyOffice), `backup`, `clamav`
- **CI/CD:** GitHub Actions, Docker Buildx with GHA cache, Trivy image scanning, Dependabot
- **Deployment:** `scripts/deploy.sh` with `--new-tunnel` for Cloudflare Quick Tunnel
- **Prod:** SSH `manuel@192.168.178.131`, Verzeichnis `~/claude/monteweb`
- **See:** [`DEPLOYMENT.md`](DEPLOYMENT.md), [`docs/INTEGRATIONS.md`](docs/INTEGRATIONS.md), [`LOCAL-DEV-GUIDE.md`](LOCAL-DEV-GUIDE.md), [`BACKUP.md`](BACKUP.md), [`docs/REVIEW-REMAINING-ITEMS.md`](docs/REVIEW-REMAINING-ITEMS.md)

### Testing

**Backend:** `@SpringBootTest @AutoConfigureMockMvc @Import(TestContainerConfig.class)` — Testcontainers spins up Postgres + Redis. `MonteWebModularityTests` verifies no illegal cross-module dependencies. JaCoCo 70% instruction minimum.

**Frontend:** Vitest + jsdom + @vue/test-utils. Setup mocks `localStorage`, PrimeVue `useToast`, and `Element.prototype.scrollTo`. Pattern: `vi.mock()` API modules, `setActivePinia(createPinia())` in `beforeEach`. 53% statement coverage threshold (actual: 56%).

**E2E:** Playwright + Chromium. 22 test files covering 296 user stories (550 tests, 171 skipped). API-based login (sessionStorage JWT injection). Run against Docker Compose app at `http://localhost`. Rate limiting disabled via `MONTEWEB_RATE_LIMIT_ENABLED=false` in `.env`.

```bash
# E2E tests (requires Docker app running at http://localhost)
cd e2e && npx playwright install chromium && npx playwright test   # all 22 files
npx playwright test tests/admin/                                    # single module
npx playwright test --reporter=list                                 # verbose output
```

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
| calendar | Events (Raum/Bereich/Schule), RSVP, Cancel→Feed, iCal-Subscriptions | `monteweb.modules.calendar.enabled` |
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

**See:** [`docs/BUSINESS-RULES.md`](docs/BUSINESS-RULES.md) for all 29 rules.

Key rules to know:
- **Familienverbund = Abrechnungseinheit.** Stunden aus Jobboerse/Putz werden Familie gutgeschrieben
- **Module abschaltbar:** Backend via `@ConditionalOnProperty`, Frontend: Menue nur wenn Modul aktiv
- **Kommunikationsregeln:** Lehrer↔Eltern immer, Eltern↔Eltern / Schueler↔Schueler: konfigurierbar
- **Audience-Sichtbarkeit:** Ordner/Fotobox-Threads: ALL, PARENTS_ONLY, STUDENTS_ONLY
- **DSGVO:** 14-Tage Loeschfrist, Datenexport, Consent-Records, DeletionListeners in allen 15 Modulen

## Conventions

- **Code:** English. **UI-Texte:** German + English (i18n). **Git:** Conventional Commits
- **Java:** Records for DTOs, `*Info` public DTOs, `*ModuleApi` facades, Lombok entities, UUIDs as PK, `Instant` for timestamps, Bean Validation on requests
- **Vue/TS:** `<script setup lang="ts">`, `@/` path alias, PascalCase components, `use`-prefix composables, types in `types/`, scoped styles

## Gotchas

- **`@EnableAsync`** muss auf `MonteWebApplication` stehen, sonst werden alle `@Async`-Methoden (DeletionListeners, Solr-Indexing) synchron ausgefuehrt -- kein Fehler, kein Warning
- **`@ApplicationModuleListener`** beinhaltet bereits `@TransactionalEventListener` + `@Transactional`. NIEMALS zusaetzlich `@Transactional` annotieren -- Spring wirft `BeanInitializationException`
- **`@Transactional(readOnly = true)`** auf Service-Klassen-Ebene: Alle mutierenden Methoden brauchen explizites `@Transactional` (ohne readOnly)
- **Java 21 nicht lokal verfuegbar:** Backend-Kompilierung nur via Docker (`docker compose build backend`)
- **FRONTEND_URL muss zur aktuellen URL passen:** Bei Cloudflare Tunnel `.env` anpassen, sonst CORS 403
- **Admin-Passwort ist `admin123`** (V032), nicht `test1234` wie andere Test-Accounts (V033/V040)
- **Modularity-Test `@Disabled`:** user↔family Zyklus (AdminUserController nutzt FamilyModuleApi). TODO: AdminUserController in admin-Modul verschieben

## API & Integrations

- **API:** `/api/v1/`, `ResponseEntity<ApiResponse<T>>`, `SecurityUtils.requireCurrentUserId()` in controllers, pagination `?page=0&size=20&sort=createdAt,desc`
- **See:** [`docs/API-REFERENCE.md`](docs/API-REFERENCE.md) for all endpoints
- **See:** [`docs/INTEGRATIONS.md`](docs/INTEGRATIONS.md) for Caddy, Cloudflare Tunnel, E-Mail, OIDC, LDAP, Jitsi, WOPI, ClamAV, Monitoring, Solr
## PindeX – Codebase Navigation

Dieses Projekt ist mit PindeX indexiert.

**PFLICHT-WORKFLOW** – bei jeder Codebase-Aufgabe:
1. **Unbekannte Datei?** → `mcp__pindex__get_file_summary` ZUERST, dann ggf. `get_context`
2. **Symbol suchen?** → `mcp__pindex__search_symbols` oder `find_symbol`
3. **Abhängigkeiten?** → `mcp__pindex__get_dependencies`
4. **Wo wird etwas verwendet?** → `mcp__pindex__find_usages`
5. **Projekt-Überblick?** → `mcp__pindex__get_project_overview`

**VERBOTEN** (solange PindeX verfügbar):
- `Read` auf Quellcode-Dateien ohne vorherigen `get_file_summary`-Aufruf
- `Glob`/`Grep` zur Symbol-Suche statt `search_symbols`

**Kontext auslagern:**
- Wichtige Entscheidungen / Muster → `mcp__pindex__save_context` speichern
- Zu Sessionbeginn → `mcp__pindex__search_docs` für gespeicherten Kontext

**Fallback:** Falls ein Tool `null` zurückgibt → `Read`/`Grep` als Fallback.
<!-- pindex -->
