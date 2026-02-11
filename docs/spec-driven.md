# MonteWeb -- Spec-Driven Documentation

**Version:** 1.0
**Date:** 2026-02-11
**Status:** All 17 phases complete, production-ready

---

## 1. Product Specification

### 1.1 Vision

MonteWeb is a modular, self-hosted school intranet for Montessori school complexes that replaces fragmented communication tools (email, WhatsApp groups, paper lists) with a single, DSGVO-compliant platform.

### 1.2 Target Users

| Persona | Needs | Frequency |
|---------|-------|-----------|
| **Parent** | View room updates, sign up for volunteer jobs, manage cleaning duties, communicate with teachers | Daily |
| **Teacher / Room Leader** | Post announcements, manage room members, create discussion threads, organize events | Daily |
| **School Administrator (SUPERADMIN)** | Manage users/roles, configure modules, view audit log, generate reports | Weekly |
| **Student** | View room content, participate in allowed discussions | Daily |
| **IT Administrator** | Deploy, monitor, configure, update the system | Monthly |

### 1.3 Core Domain Model

```
School
  └── Section (Krippe | KiGa | Grundschule | Mittelschule | Oberstufe)
        └── Room (class, group, project)
              ├── Members (LEADER | MEMBER)
              ├── Feed Posts + Comments
              ├── Discussion Threads + Replies
              ├── Files (via MinIO)
              ├── Chat (WebSocket)
              └── Calendar Events (RSVP)

Family (accounting unit)
  ├── Parents (PARENT role)
  ├── Children (STUDENT role, can belong to multiple families)
  ├── Hour Account (jobboard hours + cleaning hours)
  └── Invitations (per user search)

User
  ├── Roles: PARENT | STUDENT | TEACHER | SUPERADMIN
  ├── Rooms membership
  ├── Family membership
  └── OIDC identity (optional)
```

---

## 2. Feature Specifications

### FEAT-001: Authentication & Authorization

**What:** Secure user authentication with JWT tokens and optional OIDC/SSO.
**Why:** Protect school data, enable flexible identity providers.

**Acceptance Criteria:**
- Users register with email/password (BCrypt hashed)
- Login returns short-lived access token (15min) + refresh token (7d)
- Refresh endpoint issues new token pair
- Password reset via email link (conditional on email module)
- OIDC/SSO login via configurable OAuth2 provider (conditional on `monteweb.oidc.enabled`)
- Rate limiting on all auth endpoints

**API Contract:**
```
POST /api/v1/auth/register     -> ApiResponse<AuthInfo>
POST /api/v1/auth/login        -> ApiResponse<AuthInfo>  {accessToken, refreshToken}
POST /api/v1/auth/refresh      -> ApiResponse<AuthInfo>
POST /api/v1/auth/logout       -> ApiResponse<void>
POST /api/v1/auth/password-reset         -> ApiResponse<void>
POST /api/v1/auth/password-reset/confirm -> ApiResponse<void>
GET  /api/v1/auth/oidc/config  -> ApiResponse<OidcConfig>
POST /api/v1/auth/oidc/token   -> ApiResponse<AuthInfo>
```

**Security Rules:**
- All auth endpoints are `permitAll()`
- JWT filter validates Bearer token on all other endpoints
- `SUPERADMIN` required for `/api/v1/admin/**`
- Method-level security via `@PreAuthorize` where needed

---

### FEAT-002: User Management

**What:** User profiles, role management, DSGVO data operations.
**Why:** Central identity for all school interactions, legal compliance.

**Acceptance Criteria:**
- Users view/edit own profile
- Admin can list all users, change roles
- User search endpoint for messaging and invitation features
- Data export returns all personal data as JSON
- Account deletion anonymizes personal data irreversibly

**API Contract:**
```
GET    /api/v1/users/me              -> ApiResponse<UserInfo>
PUT    /api/v1/users/me              -> ApiResponse<UserInfo>
GET    /api/v1/users/me/data-export  -> ApiResponse<DataExport>
DELETE /api/v1/users/me              -> ApiResponse<void>
GET    /api/v1/users/{id}            -> ApiResponse<UserInfo>  (restricted fields)
GET    /api/v1/users?page=&size=     -> ApiResponse<Page<UserInfo>>  (SUPERADMIN)
GET    /api/v1/users/search?q=       -> ApiResponse<List<UserInfo>>
PUT    /api/v1/users/{id}/roles      -> ApiResponse<UserInfo>  (SUPERADMIN)
```

---

### FEAT-003: Family Management

**What:** Family groups linking parents and children with shared hour accounts.
**Why:** Volunteer hours are tracked per household, not per individual.

**Acceptance Criteria:**
- Create family group with invitation code
- Join family via code
- Invite members via user search (with role selection: PARENT/CHILD)
- View/accept/decline received invitations
- Link children to family
- Remove members
- View hour account (jobboard + cleaning subtotals)

**Data Model:**
```sql
families (id, name, invite_code, created_at)
family_members (family_id, user_id, role, joined_at)
family_invitations (id, family_id, inviter_id, invitee_id, role, status, created_at)
```

**Business Rules:**
- One parent belongs to exactly one family
- Children can belong to multiple families (shared custody)
- Hour accounts are per-family, with separate cleaning sub-account

---

### FEAT-004: School Sections

**What:** Organizational hierarchy of school divisions.
**Why:** Rooms belong to sections; events can be scoped to sections.

**Sections:** Krippe, Kindergarten, Grundschule, Mittelschule, Oberstufe

**API Contract:**
```
GET    /api/v1/sections       -> ApiResponse<List<SectionInfo>>
POST   /api/v1/sections       -> ApiResponse<SectionInfo>  (SUPERADMIN)
PUT    /api/v1/sections/{id}  -> ApiResponse<SectionInfo>
DELETE /api/v1/sections/{id}  -> ApiResponse<void>
```

---

### FEAT-005: Room Management

**What:** Core organizational unit with membership, discussions, files, chat, and events.
**Why:** Each class/group/project needs its own space for communication and resource sharing.

**Acceptance Criteria:**
- Create rooms assigned to a section
- Manage members (add/remove, roles: LEADER/MEMBER)
- Browse all rooms where user is not a member
- Send join requests to rooms; leaders approve/decline
- Discussion threads (LEADER creates/archives/deletes, members reply)
- File upload/download (via MinIO)
- Real-time chat (WebSocket)
- Room-scoped calendar events

**Room Member Roles:**
| Role | Permissions |
|------|------------|
| LEADER | All MEMBER permissions + create threads, manage members, approve join requests |
| MEMBER | View content, post, reply to threads, upload files, chat |

---

### FEAT-006: Feed System

**What:** Unified timeline of posts from rooms the user belongs to.
**Why:** Single place to catch up on all relevant school activity.

**Acceptance Criteria:**
- Posts appear from all rooms where user is a member
- Role-based visibility filtering
- Post creation with optional room assignment
- Comments on posts
- Pin/unpin posts (room leaders or admin)
- System banners (e.g., cleaning reminders for relevant parents)

---

### FEAT-007: Messaging [Conditional Module]

**What:** Direct messaging between users with communication rules.
**Why:** Replace untracked WhatsApp groups with auditable in-app messaging.

**Toggle:** `monteweb.modules.messaging.enabled` (default: `true`)

**Communication Rules:**
| Route | Default | Configurable |
|-------|---------|-------------|
| Teacher <-> Parent | Always allowed | No |
| Parent <-> Parent | Disabled | Yes (tenant config) |
| Student <-> Student | Disabled | Yes (tenant config) |

---

### FEAT-008: Job Board [Conditional Module]

**What:** Volunteer job postings with assignment tracking and hour accounting.
**Why:** Schools require parent volunteer hours; tracking needs to be transparent.

**Toggle:** `monteweb.modules.jobboard.enabled` (default: `true`)

**Acceptance Criteria:**
- Create jobs with description, hours, date, capacity
- Parents apply for jobs
- Leaders confirm completion and hours
- Hours credited to family account
- Admin report with CSV and PDF export

---

### FEAT-009: Cleaning Organization [Conditional Module]

**What:** Opt-in cleaning duty scheduling with QR code check-in.
**Why:** Track cleaning hours separately; verify attendance via QR scan.

**Toggle:** `monteweb.modules.cleaning.enabled` (default: `true`)

**Acceptance Criteria:**
- Admin creates cleaning configs and generates time slots
- Parents opt-in to available slots
- QR code generated per slot (PDF export for admin)
- Check-in/check-out via QR scan (mobile)
- Hours credited to family cleaning sub-account
- Dashboard for admin overview

---

### FEAT-010: Calendar & Events [Conditional Module]

**What:** Events at room, section, or school scope with RSVP.
**Why:** Centralize school event management with attendance tracking.

**Toggle:** `monteweb.modules.calendar.enabled` (default: `true`)

**Event Scopes & Permissions:**
| Scope | Can Create |
|-------|-----------|
| ROOM | Room LEADER or SUPERADMIN |
| SECTION | TEACHER or SUPERADMIN |
| SCHOOL | SUPERADMIN only |

**Recurrence:** NONE, DAILY, WEEKLY, MONTHLY, YEARLY
**RSVP:** ATTENDING, MAYBE, DECLINED (open to all authenticated users)

---

### FEAT-011: Notifications

**What:** Multi-channel notification system (in-app, WebSocket, Web Push).
**Why:** Users need timely alerts without polling.

**Notification Types:**
- Feed: new post, new comment
- Room: member added/removed, join request
- Discussion: new thread, new reply
- Calendar: event created, cancelled, upcoming
- Family: invitation received
- Messaging: new message
- Cleaning: slot reminder

**Channels:**
| Channel | Mechanism | Conditional |
|---------|-----------|-------------|
| In-App | Database + REST API | No |
| Real-Time | WebSocket via Redis Pub/Sub | No |
| Push | VAPID Web Push | Yes (`monteweb.push.enabled`) |
| Email | SMTP | Yes (`monteweb.email.enabled`) |

---

### FEAT-012: Admin Panel

**What:** System-wide configuration and monitoring.
**Why:** Schools need self-service administration.

**Capabilities:**
- User management (roles, search)
- Room management (create, assign to sections)
- Section management
- Module toggles (messaging, files, jobboard, cleaning, calendar)
- Theme customization (CSS custom properties, logo upload)
- Audit log viewer
- Job hour reports (CSV/PDF)
- Cleaning QR code generation

---

## 3. Technical Specifications

### 3.1 Technology Matrix

| Layer | Technology | Version |
|-------|-----------|---------|
| Runtime | Java | 21 (LTS) |
| Framework | Spring Boot | 3.4.2 |
| Module System | Spring Modulith | 1.3.2 |
| Database | PostgreSQL | 16 |
| Cache/Sessions | Redis | 7 |
| Object Storage | MinIO | Latest |
| Frontend Runtime | Vue | 3.5 |
| Type System | TypeScript | 5.9 |
| Build (Frontend) | Vite | 7.3 |
| UI Components | PrimeVue | 4.5 (Aura) |
| State | Pinia | 3 |
| Routing | Vue Router | 4.6 |
| i18n | vue-i18n | 11 |
| HTTP Client | Axios | 1.13 |
| Testing (FE) | Vitest + @vue/test-utils | 4.0 |
| Testing (BE) | JUnit 5 + Testcontainers | 1.20.4 |
| Containerization | Docker + Docker Compose | v2 |
| Reverse Proxy | nginx | Alpine |
| CI/CD | GitHub Actions | - |
| Monitoring | Prometheus + Grafana | 2.51 / 10.4 |

### 3.2 Database Schema

35 Flyway migrations (V001-V035) managing:

| Migration Range | Domain |
|----------------|--------|
| V001-V007 | Core: tenant config, users, sections, families, rooms, audit log, seed data |
| V008-V009 | Feed posts/comments, notifications |
| V010-V011 | Messaging conversations, file storage |
| V012 | Spring Modulith event publication |
| V013-V014 | Jobboard: jobs, assignments |
| V015-V016 | Cleaning: configs, slots, registrations |
| V017-V018 | Room extensions: interest fields, chat channels |
| V019-V020 | DSGVO: user deletion fields, password reset tokens |
| V021 | Communication rules |
| V022 | Discussion threads and replies |
| V023 | Push subscriptions |
| V024 | OIDC fields on users |
| V025-V026 | Calendar events, RSVP, module seed |
| V027-V032 | Schema extensions (avatars, public descriptions, admin seed) |
| V033 | Test user seed data |
| V034 | Room join requests |
| V035 | Family invitations |

### 3.3 Security Specification

**Authentication:**
- BCrypt password hashing (strength 10)
- JWT access token: 15-minute expiry, HS512 signing
- JWT refresh token: 7-day expiry, stored in Redis
- OIDC: Authorization Code flow with PKCE (optional)

**Transport Security:**
- HSTS with 1-year max-age, includeSubDomains
- CSRF disabled (stateless JWT API)
- CORS restricted to configured frontend origin

**Header Security:**
- X-Frame-Options: DENY
- X-Content-Type-Options: nosniff
- Content-Security-Policy: strict self-only policy
- Permissions-Policy: deny camera, microphone, geolocation
- Referrer-Policy: strict-origin-when-cross-origin

**Application Security:**
- Rate limiting on auth endpoints
- Input validation via Bean Validation (`@NotNull`, `@Size`)
- Parameterized queries (JPA/Hibernate)
- Method-level authorization (`@PreAuthorize`)
- Audit logging for admin actions

### 3.4 API Specification

**Base URL:** `/api/v1/`

**Response Envelope:**
```json
{
  "data": <T>,
  "message": "string",
  "success": true
}
```

**Error Response:**
```json
{
  "error": "string",
  "message": "string",
  "status": 400,
  "timestamp": "2026-02-11T10:00:00Z"
}
```

**Pagination:**
```
Request:  ?page=0&size=20&sort=createdAt,desc
Response: { content: [...], totalElements: N, totalPages: N, number: 0, size: 20 }
```

**Authentication:** `Authorization: Bearer <jwt-access-token>`

### 3.5 Module Configuration Specification

Each conditional module follows this pattern:

```yaml
# application.yml
monteweb:
  modules:
    {module-name}:
      enabled: true  # or false
```

**Implementation requirement:** Every Spring bean (`@Service`, `@Controller`, `@Component`, `@Configuration`) within the module MUST carry:

```java
@ConditionalOnProperty(prefix = "monteweb.modules", name = "{module-name}.enabled", havingValue = "true")
```

**Affected modules:** messaging, files, jobboard, cleaning, calendar, forms

---

## 4. Infrastructure Specification

### 4.1 Container Topology

```yaml
services:
  postgres:    # Data persistence (volumes: postgres_data)
  redis:       # Sessions, cache, pub/sub (volumes: redis_data)
  minio:       # File storage (volumes: minio_data)
  backend:     # Spring Boot application (depends: postgres, redis)
  frontend:    # nginx + SPA static files (depends: backend)
  prometheus:  # Metrics collection [monitoring profile]
  grafana:     # Dashboards [monitoring profile]
```

### 4.2 Network Topology

```
Internet -> [TLS Termination*] -> nginx:80
  nginx:80 -> /api/*          -> backend:8080
  nginx:80 -> /ws/*           -> backend:8080  (WebSocket upgrade)
  nginx:80 -> /actuator/*     -> backend:8080
  nginx:80 -> /*              -> static SPA files

backend:8080 -> postgres:5432  (JDBC)
backend:8080 -> redis:6379     (Lettuce)
backend:8080 -> minio:9000     (S3 protocol)

prometheus:9090 -> backend:8080/actuator/prometheus  (scrape)
grafana:3000    -> prometheus:9090                    (query)
```

*TLS termination expected to be handled by external load balancer or nginx config extension.

### 4.3 Health Checks

| Service | Endpoint/Command | Interval | Start Period |
|---------|-----------------|----------|-------------|
| postgres | `pg_isready` | 10s | - |
| redis | `redis-cli ping` | 10s | - |
| minio | `mc ready local` | 10s | - |
| backend | `wget http://localhost:8080/actuator/health` | 30s | 60s |
| frontend | `wget http://127.0.0.1:80/` | 30s | - |

### 4.4 Environment Variables

**Required:**
- `POSTGRES_PASSWORD` -- Database password
- `REDIS_PASSWORD` -- Redis password
- `JWT_SECRET` -- JWT signing key (min 64 chars)
- `MINIO_ACCESS_KEY` -- MinIO access key
- `MINIO_SECRET_KEY` -- MinIO secret key

**Optional (with defaults):**
- `POSTGRES_DB` (monteweb), `POSTGRES_USER` (monteweb)
- `APP_PORT` (80), `FRONTEND_URL` (http://localhost)
- `EMAIL_ENABLED` (false), `SMTP_*` settings
- `OIDC_ENABLED` (false), `OIDC_*` settings
- `PUSH_ENABLED` (false), `VAPID_*` settings

---

## 5. Testing Specification

### 5.1 Frontend Testing

**Tool:** Vitest 4 + @vue/test-utils + jsdom
**Coverage target:** 55% statement coverage (current)
**Test count:** 418 tests across 68 test files

| Test Category | Files | Tests | Scope |
|--------------|-------|-------|-------|
| Store unit tests | 12 | 98 | Pinia store logic |
| Component tests | 8 | 27 | Common components |
| View tests | 19 | ~150 | Page-level rendering |
| Admin view tests | 6 | ~50 | Admin panel views |
| Layout tests | 2 | ~20 | Header, sidebar, nav |
| Feature component tests | ~20 | ~73 | Feed, room, messaging, family |

### 5.2 Backend Testing

**Tool:** JUnit 5 + Spring Boot Test + Testcontainers (PostgreSQL + Redis)
**Test count:** ~100 tests across 15 test files

| Test File | Module |
|-----------|--------|
| AuthControllerIntegrationTest | auth |
| UserServiceIntegrationTest | user |
| SchoolSectionServiceTest | school |
| RoomControllerIntegrationTest | room |
| DiscussionThreadControllerIntegrationTest | room |
| FamilyControllerIntegrationTest | family |
| FeedControllerIntegrationTest | feed |
| CalendarControllerIntegrationTest | calendar |
| MessagingControllerIntegrationTest | messaging |
| JobboardControllerIntegrationTest | jobboard |
| CleaningControllerIntegrationTest | cleaning |
| NotificationServiceIntegrationTest | notification |
| FormsControllerIntegrationTest | forms |
| GlobalExceptionHandlerTest | shared |
| SecurityUtilsTest | shared |

### 5.3 CI/CD Pipeline

**Trigger:** Push to `main`, pull requests to `main`

```
┌──────────────────────────────────────────────────┐
│  CI Pipeline (.github/workflows/ci.yml)          │
│                                                    │
│  ┌──────────┐    ┌──────────┐                     │
│  │ Backend  │    │ Frontend │    (parallel)        │
│  │ Java 21  │    │ Node 22  │                     │
│  │ + Maven  │    │ + Vite   │                     │
│  │ + PG/Redis│   │          │                     │
│  └────┬─────┘    └────┬─────┘                     │
│       │               │                            │
│       └───────┬───────┘                            │
│               │                                    │
│        ┌──────┴──────┐                             │
│        │ Docker Build│  (main branch only)         │
│        │ BE + FE     │                             │
│        │ + compose   │                             │
│        └─────────────┘                             │
└──────────────────────────────────────────────────┘
```

---

## 6. Operational Specification

### 6.1 Deployment

```bash
# Production (5 core services)
cp .env.example .env       # Configure required variables
docker compose up -d

# With monitoring (7 services)
docker compose --profile monitoring up -d
```

### 6.2 Monitoring

- **Prometheus:** Scrapes `/actuator/prometheus` every 15s
- **Grafana:** Pre-provisioned dashboard with 7 panels (monteweb-overview)
- **Health endpoints:** `/actuator/health` (public), `/actuator/metrics` (authenticated)
- **Application tag:** All metrics tagged with `application=monteweb`

### 6.3 Backup Strategy [NEEDS IMPLEMENTATION]

- PostgreSQL: `pg_dump` scheduled via cron
- Redis: RDB snapshots (default persistence)
- MinIO: Volume-level backup or S3 replication
- Configuration: Version-controlled `.env` (excluding secrets)

### 6.4 Update Procedure

```bash
git pull                    # Get latest code
docker compose build        # Rebuild images
docker compose up -d        # Rolling restart
# Flyway runs automatically on backend startup
```

---

## 7. Compliance Specification

### 7.1 DSGVO (GDPR) Requirements

| Requirement | Implementation |
|-------------|---------------|
| Right to access | `GET /api/v1/users/me/data-export` |
| Right to deletion | `DELETE /api/v1/users/me` (anonymization) |
| Data minimization | Only necessary fields collected |
| Purpose limitation | Data used only for school intranet functions |
| Data sovereignty | Self-hosted, no external SaaS |
| Consent | User registration as explicit consent |
| Data portability | JSON export of all personal data |

### 7.2 Accessibility [NEEDS CLARIFICATION]

- PrimeVue components include ARIA attributes by default
- Semantic HTML structure
- Keyboard navigation support via PrimeVue
- Color contrast: depends on theme configuration

---

## 8. Evolution Roadmap

### Completed (Phases 1-17)

All core and extension features are implemented and tested.

### Potential Future Enhancements

| Enhancement | Complexity | Value |
|------------|-----------|-------|
| Database backup automation | Low | High |
| Email HTML templates | Low | Medium |
| Full-text search (Elasticsearch) | High | Medium |
| Mobile native app (Capacitor) | High | Medium |
| Multi-tenant support | High | Low (single school focus) |
| API rate limiting beyond auth | Low | Medium |
| Automated penetration testing | Medium | High |
| Horizontal scaling guide | Medium | Low |
