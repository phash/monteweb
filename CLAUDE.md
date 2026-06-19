# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MonteWeb: modulares, selbst-gehostetes Schul-Intranet fuer Montessori-Schulkomplexe (Krippe bis Oberstufe).
Raeume, Feed, Direktnachrichten, Jobboerse (Elternstunden), Putz-Organisation (QR-Check-in), Kalender, Formulare, Fotobox.

**Tech:** Java 21 + Spring Boot 4.0.6 + Spring Modulith 2.0.6 | Vue 3.5 + TS 5.9 + PrimeVue 4 Aura | PostgreSQL 16, Redis 7, MinIO, Solr 9.8 | Docker Compose + Caddy (SSL) + nginx

**20 backend modules**, 116 Flyway migrations (V001–V117), ~1965 frontend tests (56% coverage), ~693 backend tests (53 classes), Playwright E2E (~23 test files)

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

# Backend tests — Java 21 is NOT local, run inside Docker (~693 tests, 53 classes).
# CI runs against shared service-container Postgres+Redis (testcontainers OFF);
# locally, Testcontainers spins them up per Spring context. Reproduce CI exactly:
#   docker run --rm -v $PWD/backend:/work -v ~/.m2:/root/.m2 -w /work \
#     -e SPRING_DATASOURCE_URL=jdbc:postgresql://<pg-host>:5432/monteweb_test \
#     -e SPRING_DATASOURCE_USERNAME=monteweb -e SPRING_DATASOURCE_PASSWORD=testpassword \
#     -e SPRING_DATA_REDIS_HOST=<redis-host> \
#     maven:3.9-eclipse-temurin-21 mvn test -Dtestcontainers.enabled=false
# Single class/method: add -Dtest=AuthControllerIntegrationTest[#register_*]

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

**Test Accounts:** `admin@monteweb.local` / `test1234` (SUPERADMIN; V032 seeds `admin123` but V111 resets it to `test1234`), `lehrer@monteweb.local` / `test1234` (TEACHER), `eltern@monteweb.local` / `test1234` (PARENT), `schueler@monteweb.local` / `test1234` (STUDENT), `sectionadmin@monteweb.local` / `test1234` (SECTION_ADMIN). Plus ~220 seed users from V040 (all `test1234`). **Note:** On fresh prod deployments, named test accounts may need manual creation (see `docs/DEPLOYMENT.md`).

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

**Theming:** CSS custom properties `--mw-*` (defined in `assets/styles/variables.css`). PrimeVue is bridged to these tokens via `definePreset` (`MontePreset` in `main.ts`) in BOTH colour schemes, so the admin tenant theme (`useTheme.ts` ← `tenant_config.theme`) recolours custom + PrimeVue components alike. Yellow is light → primary contrast is BLACK; focus ring is dark.

**PWA:** Installable via `vite-plugin-pwa` + Workbox. Service worker with runtime caching (NetworkFirst for API calls). Icons in `public/icons/`. `usePwaInstall` composable handles install prompt with 7-day dismiss delay.

### Database

- **Flyway** V001–V117 (116 migrations). Never modify existing migrations — always create new `VXXX__description.sql`. Hibernate `ddl-auto: validate`
- UUID PKs, `TIMESTAMP WITH TIME ZONE`, PostgreSQL arrays, JSONB
- **Key gotchas:** `room_members` has composite PK (no `id`), `rooms.is_archived` (NOT `archived`), `messages.content` is nullable
- **See:** [`docs/DATABASE-SCHEMA.md`](docs/DATABASE-SCHEMA.md) for full schema reference

### Infrastructure (Docker / CI/CD)

- **Docker Compose:** 6 core services (postgres, redis, minio, solr, backend, frontend). Two isolated networks. Memory limits on all services. Optional profiles: `ssl` (Caddy), `monitoring` (Prometheus+Grafana), `office` (OnlyOffice), `backup`, `clamav`
- **CI/CD:** GitHub Actions, Docker Buildx with GHA cache, Trivy image scanning, Dependabot
- **Deployment:** `scripts/deploy.sh` with `--new-tunnel` for Cloudflare Quick Tunnel
- **Prod (Heim):** SSH `manuel@192.168.178.131`, Verzeichnis `~/claude/monteweb`
- **Live-Test-Instanz:** https://monteweb.mr-development.de — SSH `musikersuche@musikersuche.org`, Verzeichnis `/opt/monteweb` (git clone, branch main), zentrales Caddy in `/opt/caddyserver` (Container `caddy-proxy`, geteiltes Netz `caddy-proxy`). Deploy: `git pull` (lokale Edits `docker-compose.yml`/`frontend/nginx.conf` per stash erhalten) → `docker compose build frontend|backend` → `up -d`. `frontend/nginx.conf` proxyt auf `monteweb-backend:8080`. Solr-Schema-Aenderung braucht Core-Neuanlage (`docker volume rm monteweb_solr_data`) + Reindex `POST /api/v1/admin/search/reindex`
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
- **Admin-Passwort ist `test1234`** auf jeder migrierten DB (V111 ueberschreibt V032's `admin123`). Frische Prod-Deploys ohne V111-Daten ggf. anders
- **Modularity-Test `@Disabled`:** user↔family Zyklus (AdminUserController nutzt FamilyModuleApi). TODO: AdminUserController in admin-Modul verschieben
- **Backend-CI teilt EINE Postgres** (Service-Container, ganze Suite, keine Per-Klassen-Isolation). Ein Test, der Seed-Daten mutiert (z.B. den Seed-SUPERADMIN deaktiviert) ohne Cleanup, verschmutzt JEDE spaetere Klasse → CI-only-Fehler, die lokal gruen sind (Testcontainers gibt `@AutoConfigureMockMvc`-Klassen eine separate DB). Geteilte Mutationen per `@AfterEach` zuruecksetzen
- **`npm run test:coverage` (CI) ist strenger als `npm test`** — unbehandelte async-Fehler lassen Tests fehlschlagen. Ein fire-and-forget Store-Call (`store.fetchX()` on mount) mit unvollstaendigem `vi.mock` faellt NUR unter `--coverage`
- **CI-Split: `E2E` + `Docker Build` laufen NUR bei Push auf `main`** (in PR-Runs `SKIPPED`). Der `Docker Build`-Job bricht per **Trivy-Gate** (`exit-code 1`) bei HIGH/CRITICAL-CVEs im Backend-Image ab — ein grüner PR (Backend+Frontend gruen) kann `main` also trotzdem rot lassen. Branch-Protection auf `main`: 1 Review noetig, keine Pflicht-Checks, `enforce_admins` aus → Merge via `gh pr merge <n> --admin --squash`. Dependabot-Frontend-PRs teilen sich `package-lock.json` → nur sequenziell mergebar (`@dependabot rebase` konsolidiert sie ggf. in Group-PRs)

## API & Integrations

- **API:** `/api/v1/`, `ResponseEntity<ApiResponse<T>>`, `SecurityUtils.requireCurrentUserId()` in controllers, pagination `?page=0&size=20&sort=createdAt,desc`
- **See:** [`docs/API-REFERENCE.md`](docs/API-REFERENCE.md) for all endpoints
- **See:** [`docs/INTEGRATIONS.md`](docs/INTEGRATIONS.md) for Caddy, Cloudflare Tunnel, E-Mail, OIDC, LDAP, Jitsi, WOPI, ClamAV, Monitoring, Solr
## PindeX – Codebase Navigation

Dieses Projekt ist (sofern der PindeX-MCP-Server verbunden ist) mit PindeX indexiert. **Die `mcp__pindex__*`-Tools sind nicht in jeder Session verfuegbar** — wenn sie fehlen, direkt `Read`/`Grep`/`Glob` nutzen (kein Workflow-Verstoss).

**Wenn PindeX verfuegbar ist, bevorzugt nutzen:**
1. **Unbekannte Datei?** → `mcp__pindex__get_file_summary`, dann ggf. `get_context`
2. **Symbol suchen?** → `mcp__pindex__search_symbols` / `find_symbol`
3. **Abhaengigkeiten?** → `mcp__pindex__get_dependencies`
4. **Verwendungen?** → `mcp__pindex__find_usages`
5. **Ueberblick?** → `mcp__pindex__get_project_overview`
6. **Kontext:** wichtige Muster → `save_context`; Sessionbeginn → `search_docs`

**Fallback (immer ok):** PindeX nicht verfuegbar oder ein Tool gibt `null` → `Read`/`Grep`/`Glob`.
<!-- pindex -->
