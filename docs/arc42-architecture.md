# MonteWeb -- arc42 Architecture Documentation

**Version:** 1.1
**Date:** 2026-02-12
**Status:** All 19 phases complete

---

## 1. Introduction and Goals

### 1.1 Requirements Overview

MonteWeb is a modular, self-hosted school intranet for Montessori school complexes (nursery through upper secondary). It manages:

- **Rooms** (classes, groups, projects) with membership, discussions, and file sharing
- **Unified Feed** with posts, comments, and system banners
- **Direct Messaging** with configurable communication rules
- **Job Board** for parent volunteer hours with time tracking
- **Cleaning Organization** with QR check-in for parent duties
- **Calendar/Events** with RSVP at room, section, and school-wide scope
- **Forms & Surveys** with scoped distribution, anonymous/named responses, and CSV/PDF export
- **Fotobox** photo gallery threads in rooms with thumbnail generation and lightbox
- **Notifications** (in-app, WebSocket real-time, Web Push)
- **DSGVO-compliant** user data export and account deletion

### 1.2 Quality Goals

| Priority | Quality Goal | Scenario |
|----------|-------------|----------|
| 1 | **Data Privacy (DSGVO)** | Users can export all personal data and delete their account at any time. No data leaves the self-hosted instance. |
| 2 | **Modularity** | Individual modules (messaging, files, jobboard, cleaning, calendar) can be enabled/disabled without code changes via configuration properties. |
| 3 | **Security** | All endpoints require authentication. JWT with short-lived access tokens (15min). Security headers enforced at both application and reverse proxy layers. |
| 4 | **Self-Hostability** | A single `docker compose up` deploys the entire stack. No external SaaS dependencies required. |
| 5 | **Usability** | Responsive design for parents on mobile (bottom nav) and staff on desktop (sidebar). PWA support for app-like experience. |

### 1.3 Stakeholders

| Role | Expectations |
|------|-------------|
| **School Administration** | Central management of rooms, sections, users, and modules. Audit logging. |
| **Teachers / Room Leaders** | Create posts, manage room members, create discussion threads, organize events. |
| **Parents** | View feed, sign up for volunteer jobs, manage cleaning duties, join family groups, communicate with teachers. |
| **Students** | View room content, participate in discussions (with configurable messaging rules). |
| **IT Administrator** | Simple deployment via Docker, monitoring via Prometheus/Grafana, configurable features. |

---

## 2. Constraints

### 2.1 Technical Constraints

| Constraint | Rationale |
|-----------|-----------|
| Java 21, Spring Boot 3.4 | LTS version, strong ecosystem for enterprise applications |
| PostgreSQL 16 | Robust relational DB for complex domain with referential integrity |
| Self-hosted only | DSGVO compliance -- school data must not leave institution control |
| Docker-based deployment | Target audience (school IT) has limited DevOps resources |
| No external SaaS | No dependency on cloud providers for core functionality |

### 2.2 Organizational Constraints

| Constraint | Rationale |
|-----------|-----------|
| German as primary UI language | Target market is German Montessori schools |
| Montessori school hierarchy | Sections (Krippe, KiGa, Grundschule, Mittelschule, Oberstufe) are fixed domain concepts |
| Family-based accounting | Volunteer hours are tracked per family unit, not individual |
| Privacy by design | Minimum data collection, user-controlled data lifecycle |

### 2.3 Conventions

| Convention | Description |
|-----------|-------------|
| Code language: English | Variables, classes, comments in English |
| UI language: German + English | All strings via i18n keys |
| RESTful API under `/api/v1/` | Standard JSON responses wrapped in `ApiResponse<T>` |
| Conventional Commits | `feat:`, `fix:`, `chore:` prefixes |

---

## 3. Context and Scope

### 3.1 Business Context

```
                    +------------------+
                    |   School Staff   |
                    | (Teachers/Admin) |
                    +--------+---------+
                             |
                    +--------v---------+
   +---------+     |                   |     +-----------+
   | Parents  +---->    MonteWeb       <-----+ Students  |
   +---------+     |   Intranet        |     +-----------+
                    |                   |
                    +---+-------+---+---+
                        |       |   |
              +---------+  +----+  +--------+
              v            v               v
         SMTP Server   OIDC Provider   Browser/PWA
         (optional)    (optional)      (end users)
```

| Communication Partner | Data Exchanged |
|----------------------|----------------|
| **Browser / PWA** | HTML/JS SPA, REST API calls (JSON), WebSocket messages |
| **SMTP Server** | Outbound email notifications (optional, configurable) |
| **OIDC Provider** | OAuth2 authorization code flow for SSO (optional, configurable) |

### 3.2 Technical Context

```
+-----------------------------------------------------------+
|                    Docker Host                             |
|                                                            |
|  +----------+    +----------+    +---------+               |
|  | nginx    |--->| Backend  |--->|Postgres |               |
|  | :80      |    | :8080    |    | :5432   |               |
|  +----------+    +----+-----+    +---------+               |
|       |               |                                    |
|       |          +----+-----+    +---------+               |
|       |          | Redis    |    | MinIO   |               |
|       |          | :6379    |    | :9000   |               |
|       |          +----------+    +---------+               |
|       |                                                    |
|  +----+------+   +----------+    +---------+               |
|  | Frontend  |   |Prometheus|    | Grafana | (optional)    |
|  | (static)  |   | :9090    |    | :3000   |               |
|  +-----------+   +----------+    +---------+               |
+-----------------------------------------------------------+
```

| Channel | Technology | Purpose |
|---------|-----------|---------|
| HTTP/HTTPS | nginx reverse proxy | SPA delivery, API routing |
| WebSocket | Spring WebSocket + Redis Pub/Sub | Real-time notifications, chat |
| JDBC | PostgreSQL driver | Persistent data storage |
| Redis Protocol | Lettuce client | Sessions, cache, pub/sub |
| S3 Protocol | MinIO client | File storage |
| Prometheus scrape | HTTP `/actuator/prometheus` | Metrics collection |

---

## 4. Solution Strategy

| Decision | Rationale |
|----------|-----------|
| **Spring Modulith** | Enforces module boundaries at compile time while keeping deployment as single artifact. Easier than microservices for school IT. |
| **Module toggle via `@ConditionalOnProperty`** | Schools can disable unused features without code changes. All beans in optional modules carry this annotation. |
| **JWT + Redis sessions** | Stateless API authentication with short-lived access tokens (15min) and long-lived refresh tokens (7d) stored in Redis. |
| **Flyway migrations** | Schema changes are versioned (V001-V039). Hibernate only validates (`ddl-auto: validate`). |
| **Vue 3 + PrimeVue SPA** | Rich component library reduces custom UI code. PWA for mobile-first experience. |
| **Docker Compose as deployment unit** | Single command deploys all infrastructure. No Kubernetes needed for typical school deployment. |
| **Facade pattern for inter-module communication** | `*ModuleApi` interfaces provide stable contracts. Spring Events for async decoupling. |

---

## 5. Building Block View

### Level 1: System Decomposition

```
+------------------------------------------------------------------+
|                         MonteWeb                                  |
|                                                                    |
|  +--------+  +------+  +--------+  +--------+  +------+          |
|  | auth   |  | user |  | family |  | school |  | room |          |
|  +--------+  +------+  +--------+  +--------+  +------+          |
|                                                                    |
|  +--------+  +---------------+  +---------+  +----------+         |
|  | feed   |  | notification  |  | admin   |  | calendar |         |
|  +--------+  +---------------+  +---------+  +----------+         |
|                                                                    |
|  +-----------+  +-------+  +----------+  +----------+             |
|  | messaging |  | files |  | jobboard |  | cleaning |  [optional] |
|  +-----------+  +-------+  +----------+  +----------+             |
|                                                                    |
|  +---------+  +---------+                                         |
|  | forms   |  | fotobox |                              [optional] |
|  +---------+  +---------+                                         |
|                                                                    |
|  +----------------------------------------------------------------+
|  | shared (config, dto, exception, util)                          |
|  +----------------------------------------------------------------+
+------------------------------------------------------------------+
```

### Level 2: Module Internal Structure

Each module follows the same internal pattern:

```
com.monteweb.{module}/
  ├── {Module}ModuleApi.java          # Public facade interface
  ├── {Module}Info.java               # Public DTO (Java record)
  ├── *Event.java                     # Spring ApplicationEvents
  └── internal/
      ├── config/                     # Module-specific configuration
      ├── model/                      # JPA entities
      ├── repository/                 # Spring Data repositories
      ├── service/                    # Business logic
      ├── controller/                 # REST controllers
      └── dto/                        # Internal request/response DTOs
```

### Key Module Responsibilities

| Module | Responsibility | Public API |
|--------|---------------|------------|
| **auth** | Registration, login, JWT, password reset, OIDC/SSO | `AuthModuleApi` |
| **user** | Profile management, roles, DSGVO export/deletion | `UserModuleApi` |
| **family** | Family groups, invitation codes, hour accounts | `FamilyModuleApi` |
| **school** | School sections (Krippe through Oberstufe) | `SchoolModuleApi` |
| **room** | Rooms, membership, join requests, discussion threads | `RoomModuleApi` |
| **feed** | Posts, comments, system banners | `FeedModuleApi` |
| **notification** | In-app, WebSocket, Web Push notifications | `NotificationModuleApi` |
| **calendar** | Events (room/section/school scope), RSVP | `CalendarModuleApi` |
| **messaging** | Direct messages, conversations, communication rules | `MessagingModuleApi` |
| **files** | File upload/download via MinIO | `FilesModuleApi` |
| **jobboard** | Volunteer jobs, assignments, hour tracking, PDF reports | `JobboardModuleApi` |
| **cleaning** | Cleaning schedules, QR check-in, PDF QR codes | `CleaningModuleApi` |
| **forms** | Surveys, consent forms, scoped distribution, CSV/PDF export | `FormsModuleApi` |
| **fotobox** | Photo gallery threads in rooms, thumbnails, lightbox | `FotoboxModuleApi` |
| **admin** | System config, theme, modules, audit log | (internal only) |
| **shared** | CORS, security, rate limiting, error handling, PDF util | `@NamedInterface` exports |

---

## 6. Runtime View

### 6.1 User Authentication Flow

```
Browser          nginx          Backend (auth)        Redis         PostgreSQL
  |                |                |                    |               |
  |-- POST /api/v1/auth/login ---->|                    |               |
  |                |                |-- validate creds ----------------->|
  |                |                |<-- user record --------------------|
  |                |                |-- store refresh token -->|         |
  |                |                |<-- OK --------------------|         |
  |<-- { accessToken, refreshToken } --|                    |               |
  |                |                |                    |               |
  |-- GET /api/v1/feed (Bearer) -->|                    |               |
  |                |                |-- validate JWT     |               |
  |                |                |-- query feed ---------------------->|
  |<-- { data: [...] } ------------|                    |               |
```

### 6.2 Real-Time Notification Flow

```
Backend (any module)     notification module     Redis Pub/Sub     WebSocket     Browser
       |                        |                     |               |            |
       |-- publish event ------>|                     |               |            |
       |                        |-- store in DB       |               |            |
       |                        |-- publish to Redis ->|               |            |
       |                        |                     |-- broadcast -->|            |
       |                        |                     |               |-- push ---->|
```

### 6.3 Module Toggle Flow

```
Admin UI        Backend (admin)       Spring Context
   |                 |                      |
   |-- PUT /api/v1/admin/config/modules -->|
   |                 |-- update DB -------->|
   |                 |                      |
   |  (on next startup)                    |
   |                 |                      |-- @ConditionalOnProperty
   |                 |                      |-- skip disabled module beans
```

---

## 7. Deployment View

### 7.1 Production Deployment

```
+------------------------------------------------------------------+
|  Docker Host (single server)                                      |
|                                                                    |
|  docker-compose.yml                                               |
|  ┌────────────────────────────────────────────────────────────┐   |
|  │                                                            │   |
|  │  ┌──────────┐    ┌──────────┐    ┌──────────┐            │   |
|  │  │ Frontend │    │ Backend  │    │ Postgres │            │   |
|  │  │ (nginx)  │───>│ (JRE 21) │───>│   16     │            │   |
|  │  │ :80      │    │ :8080    │    │ :5432    │            │   |
|  │  └──────────┘    └────┬─────┘    └──────────┘            │   |
|  │                       │                                    │   |
|  │                  ┌────┴─────┐    ┌──────────┐            │   |
|  │                  │ Redis 7  │    │ MinIO    │            │   |
|  │                  │ :6379    │    │ :9000    │            │   |
|  │                  └──────────┘    └──────────┘            │   |
|  │                                                            │   |
|  │  [monitoring profile]                                      │   |
|  │  ┌──────────┐    ┌──────────┐                             │   |
|  │  │Prometheus│    │ Grafana  │                             │   |
|  │  │ :9090    │───>│ :3000    │                             │   |
|  │  └──────────┘    └──────────┘                             │   |
|  └────────────────────────────────────────────────────────────┘   |
+------------------------------------------------------------------+
```

### 7.2 Infrastructure Requirements

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| CPU | 2 cores | 4 cores |
| RAM | 4 GB | 8 GB |
| Disk | 20 GB | 50 GB+ (depends on file uploads) |
| Docker | 24.0+ | Latest stable |
| Docker Compose | v2 | Latest stable |
| OS | Any Docker-capable Linux | Ubuntu 22.04 LTS |

### 7.3 Multi-Stage Build Process

```
Backend Dockerfile:
  Stage 1: maven:3-eclipse-temurin-21 → compile + package
  Stage 2: eclipse-temurin:21-jre → runtime only

Frontend Dockerfile:
  Stage 1: node:22-alpine → npm ci + vite build
  Stage 2: nginx:alpine → serve static files
```

---

## 8. Crosscutting Concepts

### 8.1 Authentication & Authorization

- **JWT-based stateless authentication** with BCrypt password hashing
- Access tokens expire after 15 minutes, refresh tokens after 7 days
- Roles: `SUPERADMIN`, `SECTION_ADMIN`, `TEACHER`, `PARENT`, `STUDENT`
- Special roles: `ELTERNBEIRAT`, `PUTZORGA` (string-based, stored in `specialRoles` array)
- Method-level security via `@EnableMethodSecurity`
- Optional OIDC/SSO via Spring OAuth2 Client

### 8.2 API Design

- All endpoints under `/api/v1/`
- Uniform response wrapper: `ApiResponse<T>` with `data`, `message`, `success`
- Spring Data pagination: `?page=0&size=20&sort=createdAt,desc`
- ISO 8601 date format, UTC timestamps (`Instant`)

### 8.3 Error Handling

- Global exception handler (`GlobalExceptionHandler`) maps exceptions to HTTP status codes
- Custom exceptions: `ResourceNotFoundException`, `AccessDeniedException`, `BadRequestException`
- Structured error responses via `ErrorResponse`

### 8.4 Internationalization (i18n)

- Frontend: `vue-i18n` with `de.ts` (~375+ keys) and `en.ts`
- Browser locale detection with manual override via LanguageSwitcher
- No hardcoded strings in `.vue` files

### 8.5 Security Headers

Applied at both Spring Security and nginx levels:

| Header | Value |
|--------|-------|
| `X-Frame-Options` | `DENY` |
| `X-Content-Type-Options` | `nosniff` |
| `HSTS` | `max-age=31536000; includeSubDomains` |
| `Referrer-Policy` | `strict-origin-when-cross-origin` |
| `Permissions-Policy` | `camera=(), microphone=(), geolocation=()` |
| `Content-Security-Policy` | `default-src 'self'; script-src 'self'; ...` |

### 8.6 Rate Limiting

- `RateLimitFilter` on authentication endpoints to prevent brute-force attacks

### 8.7 DSGVO Compliance

- `GET /api/v1/users/me/data-export` -- full personal data export as JSON
- `DELETE /api/v1/users/me` -- account anonymization/deletion
- User deletion fields tracked in migration V019

### 8.8 Conditional Module Loading

```java
@ConditionalOnProperty(prefix = "monteweb.modules", name = "xyz.enabled", havingValue = "true")
```

Every bean (Service, Controller, Component) in optional modules carries this annotation. Modules can be toggled at runtime via admin config changes (effective on next restart).

### 8.9 Testing Strategy

| Layer | Tool | Coverage |
|-------|------|----------|
| Frontend Unit/Component | Vitest + @vue/test-utils | 565 tests, 79 test files |
| Backend Integration | Spring Boot Test + Testcontainers | 37 test files |
| CI/CD | GitHub Actions | Backend, frontend, Docker build jobs |

---

## 9. Architectural Decisions

### ADR-1: Spring Modulith over Microservices

**Context:** School IT departments have limited DevOps expertise.
**Decision:** Use Spring Modulith for module boundary enforcement within a single deployable artifact.
**Consequences:** Simpler deployment (one JAR), module boundaries enforced by framework, trade-off is no independent scaling per module.

### ADR-2: JWT over Server-Side Sessions

**Context:** Need stateless API for SPA and PWA clients.
**Decision:** Short-lived JWT access tokens (15min) with Redis-stored refresh tokens (7d).
**Consequences:** Horizontal scalability without session affinity. Refresh token rotation prevents replay attacks.

### ADR-3: Conditional Modules via Spring Properties

**Context:** Different schools need different feature sets.
**Decision:** `@ConditionalOnProperty` on all beans in optional modules.
**Consequences:** Zero-code configuration of features. Disabled modules have zero runtime footprint.

### ADR-4: Family as Accounting Unit

**Context:** Volunteer hours need to be tracked at household level (both parents contribute).
**Decision:** `Family` entity owns the hour account. Jobs and cleaning hours credit the family, not the individual.
**Consequences:** Children in shared custody can belong to multiple families. Hour reports aggregate per family.

### ADR-5: Flyway-Only Schema Management

**Context:** Need reproducible database state across environments.
**Decision:** All schema changes as Flyway migrations (V001-V039). Hibernate set to `validate` only.
**Consequences:** Explicit migration history. No surprise schema changes. Rollback requires manual down-migration.

### ADR-6: Self-Hosted with No External Dependencies

**Context:** German school data privacy regulations (DSGVO) require data sovereignty.
**Decision:** All services (DB, cache, file storage, monitoring) run on-premise via Docker.
**Consequences:** No vendor lock-in. Schools maintain full control. Trade-off: schools must manage their own backups and updates.

### ADR-7: PrimeVue as Component Library

**Context:** Need comprehensive UI components without building from scratch.
**Decision:** PrimeVue 4 with Aura theme, individual component imports.
**Consequences:** Consistent UI, reduced development time. Theme customizable via CSS custom properties (`--mw-*`).

---

## 10. Quality Requirements

### 10.1 Quality Tree

```
Quality
├── Data Privacy (DSGVO)
│   ├── Data export within seconds
│   ├── Account deletion complete and irreversible
│   └── No data leaves institution network
├── Security
│   ├── OWASP Top 10 mitigations
│   ├── JWT token rotation
│   └── Rate limiting on auth endpoints
├── Modularity
│   ├── Feature toggle without code change
│   ├── Module boundary enforcement
│   └── Independent module testing
├── Usability
│   ├── Mobile-first responsive design
│   ├── PWA installable
│   └── Bilingual UI (DE/EN)
├── Operability
│   ├── Single-command deployment
│   ├── Health checks on all services
│   └── Prometheus/Grafana monitoring
└── Maintainability
    ├── 565 frontend tests across 79 files
    ├── 37 backend test files with Testcontainers
    └── CI/CD pipeline with automated checks
```

### 10.2 Quality Scenarios

| ID | Quality | Scenario | Measure |
|----|---------|----------|---------|
| QS-1 | Privacy | User requests data export | Response within 5 seconds with complete JSON |
| QS-2 | Security | Brute-force login attempt | Rate limiter blocks after N attempts |
| QS-3 | Availability | Backend restart | Health check recovers within 60s (start_period) |
| QS-4 | Modularity | School disables messaging | Toggle config property, restart, messaging endpoints return 404 |
| QS-5 | Performance | 200 concurrent users | Connection pool (20 max), Redis caching handles load |
| QS-6 | Deployability | New school deploys MonteWeb | `docker compose up -d` with `.env` file, operational in < 5 minutes |

---

## 11. Risks and Technical Debt

### 11.1 Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Single-server deployment has no HA | Medium | High | Document backup strategy. Future: add replication guide. |
| JWT secret compromise | Low | Critical | Require 64+ character secret. Document rotation procedure. |
| Database grows unbounded (file metadata, audit log) | Medium | Medium | Add retention policies for audit log. MinIO handles file storage separately. |
| Spring Modulith version compatibility | Low | Medium | Pin to 1.3.2. Test upgrades in CI before applying. |

### 11.2 Technical Debt

| Item | Description | Priority |
|------|-------------|----------|
| No database backup automation | Docker volumes are not backed up automatically | High |
| Frontend coverage target | Coverage thresholds to be monitored in CI | Medium |
| No API rate limiting beyond auth endpoints | Other endpoints unprotected against abuse | Medium |
| No email templates | Email content is hardcoded strings, not HTML templates | Low |
| No database connection pooling metrics | HikariCP metrics not exposed to Prometheus | Low |
| Manual module toggle requires restart | No hot-reload of module configuration | Low |

---

## 12. Glossary

| Term | Definition |
|------|-----------|
| **Bereich / Section** | School division: Krippe (nursery), KiGa (kindergarten), Grundschule (primary), Mittelschule (middle), Oberstufe (upper secondary) |
| **Raum / Room** | Organizational unit -- can be a class, working group, or project. Has members with roles (LEADER, MEMBER). |
| **Familienverbund / Family** | Household unit grouping parents and children. Accounting unit for volunteer hours. |
| **Elternstunden / Parent Hours** | Mandatory volunteer hours per family, tracked via jobboard and cleaning modules. |
| **Putz-Orga / Cleaning Organization** | Opt-in cleaning duty system with QR code check-in for time tracking. |
| **Feed** | Unified timeline showing posts from rooms the user is a member of, filtered by role. |
| **Banner** | System-wide announcements displayed at the top of the feed (e.g., cleaning reminders). |
| **LEADER** | Room role with elevated permissions: create threads, manage members, approve join requests. |
| **SUPERADMIN** | System-wide administrative role with access to admin panel, audit log, and all modules. |
| **DSGVO** | Datenschutz-Grundverordnung (GDPR) -- EU data protection regulation. |
| **OIDC** | OpenID Connect -- protocol for federated authentication (SSO). |
| **VAPID** | Voluntary Application Server Identification -- protocol for Web Push notifications. |
| **SECTION_ADMIN** | Section-level administrative role managing a specific school division (e.g., Grundschule). |
| **ELTERNBEIRAT** | Special role (string-based) granting parents extended permissions for events, forms, posts, and cleaning administration. |
| **PUTZORGA** | Special role (string-based, section-scoped) granting cleaning administration permissions for a specific section. |
| **JoinPolicy** | Room setting controlling how users join: OPEN, REQUEST (default), or INVITE_ONLY. |
| **DiscussionMode** | Room setting controlling discussion behavior: FULL, ANNOUNCEMENTS_ONLY, or DISABLED. |
| **ThreadAudience** | Discussion thread visibility scope: ALLE (all members), ELTERN (parents + staff), or KINDER (students + staff). |
| **Fotobox** | Photo gallery feature within rooms, with permission levels (VIEW_ONLY, POST_IMAGES, CREATE_THREADS). |
| **Conditional Module** | Feature module that can be enabled/disabled via `monteweb.modules.{name}.enabled` property. Currently: messaging, files, jobboard, cleaning, calendar, forms, fotobox. |
| **Spring Modulith** | Framework enforcing module boundaries within a monolithic Spring application. |
| **Facade / ModuleApi** | Public interface exposing a module's capabilities to other modules. |
