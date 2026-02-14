# MonteWeb -- Spec-Driven Documentation

**Version:** 1.4
**Date:** 2026-02-13
**Status:** All 19 phases complete + post-phase features, production-ready

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
              ├── Chat (WebSocket, channels: MAIN/PARENTS/STUDENTS)
              ├── Calendar Events (RSVP)
              ├── Fotobox (photo threads, thumbnails, lightbox, audience visibility)
              └── Forms/Surveys (multi-section scoping, dashboard widget, CSV/PDF export)

Family (accounting unit)
  ├── Parents (PARENT role)
  ├── Children (STUDENT role, can belong to multiple families)
  ├── Hour Account (jobboard hours + cleaning hours)
  └── Invitations (per user search)

User
  ├── Roles: SUPERADMIN | SECTION_ADMIN | TEACHER | PARENT | STUDENT
  ├── Assigned Roles: subset of {TEACHER, PARENT, SECTION_ADMIN} for role switching
  ├── Special roles: ELTERNBEIRAT, PUTZORGA (optional, string-based)
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
PUT    /api/v1/users/me/active-role  -> ApiResponse<TokenResponse>  (switch active role, returns new JWT)
GET    /api/v1/users/me/data-export  -> ApiResponse<DataExport>
DELETE /api/v1/users/me              -> ApiResponse<void>
GET    /api/v1/users/{id}            -> ApiResponse<UserInfo>  (restricted fields)
GET    /api/v1/users?page=&size=     -> ApiResponse<Page<UserInfo>>  (SUPERADMIN)
GET    /api/v1/users/search?q=       -> ApiResponse<List<UserInfo>>
PUT    /api/v1/users/{id}/roles      -> ApiResponse<UserInfo>  (SUPERADMIN)
PUT    /api/v1/admin/users/{id}/assigned-roles -> ApiResponse<UserInfo>  (SUPERADMIN)
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
- Auto-folder creation: when a KLASSE room is created, the files module automatically creates a default folder via `RoomCreatedEvent`
- File upload/download (via MinIO) with folder audience visibility (ALL, PARENTS_ONLY, STUDENTS_ONLY)
- Real-time chat (WebSocket)
- Room-scoped calendar events

**Room Member Roles:**
| Role | Permissions |
|------|------------|
| LEADER | All MEMBER permissions + create threads, manage members, approve join requests, configure fotobox |
| MEMBER | View content, post, reply to threads, upload files, chat (MAIN + STUDENTS channel) |
| PARENT_MEMBER | Like MEMBER but with access to PARENTS chat channel instead of STUDENTS, sees ELTERN-audience threads |
| GUEST | Read-only access to room content |

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
| ROOM | Room LEADER, SUPERADMIN, or ELTERNBEIRAT |
| SECTION | TEACHER, SECTION_ADMIN, SUPERADMIN, or ELTERNBEIRAT |
| SCHOOL | SUPERADMIN or ELTERNBEIRAT |

**Recurrence:** NONE, DAILY, WEEKLY, MONTHLY, YEARLY
**RSVP:** ATTENDING, MAYBE, DECLINED (open to all authenticated users)

---

### FEAT-011: Forms & Surveys [Conditional Module]

**What:** Scoped forms and surveys with multi-section targeting, dashboard integration, anonymous/named responses and export.
**Why:** Schools need digital consent forms, feedback surveys, and parent polls distributed to specific sections.

**Toggle:** `monteweb.modules.forms.enabled` (default: `true`)

**Form Types:**
- **SURVEY**: Questionnaire (can be anonymous)
- **CONSENT**: Yes/No consent form (always named)

**Question Types:** TEXT, SINGLE_CHOICE, MULTIPLE_CHOICE, RATING, YES_NO

**Form Scopes & Permissions:**
| Scope | Can Create | Target |
|-------|-----------|--------|
| ROOM | Room LEADER, SUPERADMIN, ELTERNBEIRAT | Single room |
| SECTION | TEACHER, SECTION_ADMIN, SUPERADMIN, ELTERNBEIRAT | Multiple sections (MultiSelect) |
| SCHOOL | SUPERADMIN, ELTERNBEIRAT | All users |

**Multi-Section Support (V046):**
- SECTION-scoped forms store `section_ids UUID[]` for multi-section targeting
- Form creation UI shows MultiSelect for school sections when SECTION scope is selected
- Available forms query uses `unnest(section_ids)` to match users' section memberships
- Section names resolved and displayed on form detail and forms list views

**Dashboard Widget:**
- `DashboardFormsWidget` shows up to 5 pending (not yet responded) forms on the dashboard
- Displays form title, type tag, scope label, and deadline
- Links to form detail view; "Alle anzeigen" links to forms list
- Only visible when forms module is enabled

**Form Lifecycle:** DRAFT -> PUBLISHED -> CLOSED -> ARCHIVED

**API Contract:**
```
GET    /api/v1/forms                 -> Available forms (paginated, filtered by user's rooms/sections)
GET    /api/v1/forms/mine            -> Own forms
POST   /api/v1/forms                 -> Create form (sectionIds[] for SECTION scope)
GET    /api/v1/forms/{id}            -> Form detail with questions
PUT    /api/v1/forms/{id}            -> Edit form (DRAFT only)
DELETE /api/v1/forms/{id}            -> Delete form (DRAFT only)
POST   /api/v1/forms/{id}/publish    -> Publish form
POST   /api/v1/forms/{id}/close      -> Close form
POST   /api/v1/forms/{id}/respond    -> Submit response
GET    /api/v1/forms/{id}/results    -> Aggregated results (creator/admin)
GET    /api/v1/forms/{id}/responses  -> Individual responses (non-anonymous, creator/admin)
GET    /api/v1/forms/{id}/results/csv -> CSV export
GET    /api/v1/forms/{id}/results/pdf -> PDF export
```

---

### FEAT-012: Fotobox [Conditional Module]

**What:** Photo gallery threads within rooms with upload, thumbnails, lightbox viewer, and audience-based visibility.
**Why:** Teachers and parents want to share event photos in an organized, room-scoped gallery with role-appropriate visibility.

**Toggle:** `monteweb.modules.fotobox.enabled` (default: `true`)

**Audience Visibility (V045):**
| Audience | Visible To |
|----------|-----------|
| ALL | All room members |
| PARENTS_ONLY | Parents, teachers, leaders, admins |
| STUDENTS_ONLY | Students, teachers, leaders, admins |

- Parents creating threads: audience auto-set to `PARENTS_ONLY` (no selector shown)
- Teachers/Leaders/Admins creating threads: choose from ALL, PARENTS_ONLY, STUDENTS_ONLY

**Permission Levels (hierarchical):**
| Level | Can Do |
|-------|--------|
| VIEW_ONLY | View photo threads and images, use lightbox |
| POST_IMAGES | Upload images to existing threads (max 20 per request) |
| CREATE_THREADS | Create new photo threads |

**Permission Resolution:**
- SUPERADMIN -> always CREATE_THREADS
- Room LEADER -> always CREATE_THREADS
- Other room members -> room default permission (`FotoboxRoomSettings.defaultPermission`)

**Image Access:** JWT token accepted via `?token=` query parameter (since `<img>` tags cannot send Authorization headers).

**API Contract:**
```
GET    /api/v1/rooms/{roomId}/fotobox/settings                    -> Room fotobox settings
PUT    /api/v1/rooms/{roomId}/fotobox/settings                    -> Update settings (Leader/Admin)
GET    /api/v1/rooms/{roomId}/fotobox/threads                     -> All photo threads in room
POST   /api/v1/rooms/{roomId}/fotobox/threads                     -> Create thread (CREATE_THREADS)
GET    /api/v1/rooms/{roomId}/fotobox/threads/{threadId}          -> Thread detail
PUT    /api/v1/rooms/{roomId}/fotobox/threads/{threadId}          -> Edit thread (owner/leader)
DELETE /api/v1/rooms/{roomId}/fotobox/threads/{threadId}          -> Delete thread (owner/leader)
GET    /api/v1/rooms/{roomId}/fotobox/threads/{threadId}/images   -> Images in thread
POST   /api/v1/rooms/{roomId}/fotobox/threads/{threadId}/images   -> Upload images (POST_IMAGES)
PUT    /api/v1/fotobox/images/{imageId}                           -> Edit image (caption, sort)
DELETE /api/v1/fotobox/images/{imageId}                           -> Delete image (owner/leader)
GET    /api/v1/fotobox/images/{imageId}                           -> Download image (+?token=)
GET    /api/v1/fotobox/images/{imageId}/thumbnail                 -> Download thumbnail (+?token=)
```

---

### FEAT-013: Notifications

**What:** Multi-channel notification system (in-app, WebSocket, Web Push, Email).
**Why:** Users need timely alerts without polling.

**Notification Types (from `NotificationType` enum):**
- `POST`, `COMMENT` -- Feed: new post, new comment
- `ROOM_JOIN_REQUEST`, `ROOM_JOIN_APPROVED`, `ROOM_JOIN_DENIED` -- Room join requests
- `DISCUSSION_THREAD`, `DISCUSSION_REPLY` -- Discussion: new thread, new reply
- `EVENT_CREATED`, `EVENT_UPDATED`, `EVENT_CANCELLED` -- Calendar events
- `FORM_PUBLISHED`, `CONSENT_REQUIRED` -- Forms/Surveys
- `FAMILY_INVITATION`, `FAMILY_INVITATION_ACCEPTED` -- Family invitations
- `MESSAGE` -- New direct message
- `CLEANING_COMPLETED`, `JOB_COMPLETED` -- Task completion
- `SYSTEM`, `REMINDER` -- System messages and reminders

**Channels:**
| Channel | Mechanism | Conditional |
|---------|-----------|-------------|
| In-App | Database + REST API | No |
| Real-Time | WebSocket via Redis Pub/Sub | No |
| Push | VAPID Web Push | Yes (`monteweb.push.enabled`) |
| Email | SMTP | Yes (`monteweb.email.enabled`) |

**Additional API:**
```
DELETE /api/v1/notifications/{id}    -> ApiResponse<void>  (delete single notification)
```

---

### FEAT-014: Admin Panel

**What:** System-wide configuration and monitoring.
**Why:** Schools need self-service administration.

**Capabilities:**
- User management (roles, special roles, search)
- Room management (create, assign to sections)
- Section management
- Module toggles (messaging, files, jobboard, cleaning, calendar, forms, fotobox)
- Theme customization (CSS custom properties, logo upload)
- Communication rules (parent-to-parent, student-to-student messaging)
- Audit log viewer
- Job hour reports (CSV/PDF)
- Cleaning QR code generation

---

### FEAT-015: Multi-Role Support

**What:** Users (except SUPERADMIN and STUDENT) can have multiple assigned roles and switch between them at runtime.
**Why:** In Montessori schools, a person can be both a teacher and a parent. They need to switch perspective without separate accounts.

**Acceptance Criteria:**
- Users have an `assigned_roles` field storing which roles they can switch to (TEACHER, PARENT, SECTION_ADMIN)
- Active role (`role`) determines all permissions — existing permission checks remain unchanged
- Users with multiple assigned roles see a role badge in the header that opens a role switcher
- Switching role issues new JWT tokens (access + refresh) with the new active role
- ProfileView shows role switcher card when user has multiple assigned roles
- SUPERADMIN can assign roles to users via multi-select checkboxes in admin user edit dialog
- SUPERADMIN and STUDENT are fixed-role users (no multi-role)

**API Contract:**
```
PUT /api/v1/users/me/active-role           -> ApiResponse<TokenResponse>  {accessToken, refreshToken, userId, email, role}
PUT /api/v1/admin/users/{id}/assigned-roles -> ApiResponse<UserInfo>      (SUPERADMIN only)
```

**Data Model:**
```sql
-- V042: assigned_roles column on users table
ALTER TABLE users ADD COLUMN assigned_roles text[] NOT NULL DEFAULT '{}';
-- Backfill: existing users get their current role as assigned role
-- SUPERADMIN and STUDENT keep empty assigned_roles (fixed-role users)
```

**Business Rules:**
- Only TEACHER, PARENT, SECTION_ADMIN are valid assignable roles
- If active `role` is removed from `assignedRoles`, user is switched to the first remaining assigned role
- Role switch returns new JWT tokens — frontend stores them and re-fetches user profile
- Sidebar, BottomNav, and all permission-based UI automatically update via reactive auth store computeds

**Migration:** V042

---

### FEAT-016: Annual Billing / Jahresabrechnung

**What:** Aggregated billing reports for family volunteer hours across configurable billing periods.
**Why:** Schools need annual summaries of parent volunteer hour compliance for administrative and reporting purposes.

**Acceptance Criteria:**
- Billing periods track family hours per year/month with status (OPEN/CLOSED)
- Admin can generate billing reports for specific periods
- Report aggregates jobboard hours + cleaning hours per family
- PDF and CSV export of billing data

**API Contract:**
```
GET /api/v1/billing/periods                  -> ApiResponse<List<BillingPeriod>>
GET /api/v1/billing/report?period=...        -> ApiResponse<BillingReport>
GET /api/v1/jobs/export/pdf?period=...       -> PDF export
GET /api/v1/jobs/export/csv?period=...       -> CSV export
```

**Data Model:**
```sql
-- V047: billing_periods
billing_periods (id, family_id, year, month, status, created_at)
-- status: OPEN | CLOSED
```

**Migration:** V047

---

### FEAT-017: Error Reporting

**What:** Automated frontend and backend error capture with fingerprint-based deduplication and optional GitHub Issue integration.
**Why:** Schools running self-hosted instances need visibility into application errors without requiring technical monitoring expertise.

**Acceptance Criteria:**
- Frontend errors submitted to public endpoint (rate-limited, 10/min per IP)
- Fingerprint computed from exception class + message for deduplication
- Duplicate errors increment occurrence count instead of creating new records
- Admin views error reports with status management (NEW -> REPORTED -> RESOLVED/IGNORED)
- Optional GitHub Issue creation via configured `github_repo` + `github_pat` in tenant config
- Backend unhandled exceptions captured via `GlobalExceptionHandler` publishing `UnhandledExceptionEvent`

**API Contract:**
```
POST /api/v1/error-reports                   -> ApiResponse<void>  (public, rate-limited)
GET  /api/v1/admin/error-reports             -> ApiResponse<Page<ErrorReport>>  (SUPERADMIN)
PUT  /api/v1/admin/error-reports/{id}/status  -> ApiResponse<ErrorReport>  (SUPERADMIN)
```

**Data Model:**
```sql
-- V048: error_reports
error_reports (id, fingerprint, exception_class, message, stack_trace, location,
              status, github_issue_url, occurrence_count, last_occurrence_at, created_at)
-- fingerprint: UNIQUE, hash of exception_class + message
-- status: NEW | REPORTED | RESOLVED | IGNORED
```

**Migration:** V048

---

### FEAT-018: Section Admin Panel

**What:** Dedicated admin interface for SECTION_ADMIN users to manage their assigned school sections.
**Why:** Section administrators need a focused view for managing rooms and members within their sections without requiring full SUPERADMIN access.

**Acceptance Criteria:**
- SECTION_ADMIN users see a dedicated section admin panel
- View and manage rooms within assigned sections
- View members of rooms in assigned sections
- Section overview with room counts and member statistics

**API Contract:**
```
GET /api/v1/section-admin/rooms              -> ApiResponse<List<RoomInfo>>
GET /api/v1/section-admin/overview           -> ApiResponse<SectionOverview>
```

**Migration:** V049 (fix section admin special roles format)

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

50 Flyway migrations (V001-V050) managing:

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
| V027 | Job-event link (jobs linked to calendar events) |
| V028-V029 | Forms module: forms, questions, responses, default module config |
| V030 | Configurable cleaning hours target |
| V031-V032 | Avatars, public room descriptions, default admin seed |
| V033 | Test user seed data |
| V034 | Room join requests |
| V035 | Family invitations |
| V036 | Thread audience (discussion thread visibility scoping) |
| V037 | Role concept refactoring (JoinPolicy, DiscussionMode, special roles, room subscriptions) |
| V038-V039 | Fotobox tables (room settings, threads, images) and schema fixes |
| V040 | Realistic seed data (~220 users) |
| V041 | Feedback Batch 1 fixes |
| V042 | Multi-role support (assigned_roles column on users) |
| V043 | Cleaning: specific_date for one-time Putzaktionen, Bundesland config for holidays |
| V044 | Feed: target_user_ids UUID[] for targeted feed posts |
| V045 | Audience visibility for room_folders and fotobox_threads |
| V046 | Multi-section forms: section_ids UUID[] with GIN index |
| V047 | Billing periods (Jahresabrechnung) — family billing with year/month/status |
| V048 | Error reports — fingerprint dedup, status tracking, GitHub Issue integration |
| V049 | Fix SECTION_ADMIN special roles format (section-scoped) |
| V050 | Enable all modules by default in tenant config |

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

**Affected modules:** messaging, files, jobboard, cleaning, calendar, forms, fotobox

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
**Test count:** 897 tests across 108 test files

| Test Category | Files | Scope |
|--------------|-------|-------|
| Store unit tests | 16 | Pinia store logic (auth, auth-multirole, feed, rooms, family, calendar, forms, fotobox, etc.) |
| Component tests | 8 | Common components (PageTitle, EmptyState, LanguageSwitcher, etc.) |
| View tests | 22 | Page-level rendering (Dashboard, Rooms, Calendar, Forms, etc.) |
| Admin view tests | 7 | Admin panel views |
| Layout tests | 3 | Sidebar, BottomNav, AppHeader (with role badge/switcher tests) |
| Feature component tests | 18 | Feed, room, messaging, family, fotobox |
| API/composable tests | 4 | Fotobox API, router, useWebSocket, usePushNotifications, useTheme |
| Type tests | 1 | Room type definitions |

### 5.2 Backend Testing

**Tool:** JUnit 5 + Spring Boot Test + Testcontainers (PostgreSQL + Redis)
**Test files:** 37 test files

| Test File | Module |
|-----------|--------|
| AuthControllerIntegrationTest | auth |
| AuthServiceIntegrationTest | auth |
| UserServiceIntegrationTest | user |
| UserControllerIntegrationTest | user |
| SchoolSectionServiceTest | school |
| SchoolSectionControllerIntegrationTest | school |
| RoomControllerIntegrationTest | room |
| RoomServiceIntegrationTest | room |
| DiscussionThreadControllerIntegrationTest | room |
| RoleConceptIntegrationTest | room |
| FamilyControllerIntegrationTest | family |
| FamilyServiceIntegrationTest | family |
| FeedControllerIntegrationTest | feed |
| FeedServiceIntegrationTest | feed |
| CalendarControllerIntegrationTest | calendar |
| CalendarServiceIntegrationTest | calendar |
| MessagingControllerIntegrationTest | messaging |
| MessagingServiceIntegrationTest | messaging |
| JobboardControllerIntegrationTest | jobboard |
| JobboardServiceIntegrationTest | jobboard |
| CleaningControllerIntegrationTest | cleaning |
| CleaningServiceIntegrationTest | cleaning |
| NotificationServiceIntegrationTest | notification |
| NotificationControllerIntegrationTest | notification |
| FormsControllerIntegrationTest | forms |
| FormsServiceIntegrationTest | forms |
| FotoboxControllerIntegrationTest | fotobox |
| AdminConfigControllerIntegrationTest | admin |
| AdminModuleApiTest | admin |
| GlobalExceptionHandlerTest | shared |
| ExceptionHandlerTest | shared |
| SecurityUtilsTest | shared |
| RateLimitFilterTest | shared |
| ApiResponseTest | shared |
| PdfServiceTest | shared |
| AvatarUtilsTest | shared |

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

### Completed (Phases 1-19)

All core and extension features are implemented and tested, including:
- Phases 1-6: Core modules (Auth, User, Family, School, Room, Feed, Notifications, Messaging, Files, Jobboard, Cleaning, i18n, Security, DSGVO)
- Phase 7-8: Messaging inbox with user picker, room discussion threads
- Phase 9-10: Email (SMTP), English translation + language switcher
- Phase 11-12: Test coverage (889 FE tests, 37 BE test files), CI/CD pipeline
- Phase 13-14: OIDC/SSO, PDF export
- Phase 15-16: Web Push notifications, Prometheus/Grafana monitoring
- Phase 17: Calendar/Events with RSVP
- Phase 18: Forms & Surveys (Survey/Consent, scopes, anonymous/named, CSV/PDF export)
- Phase 19: Fotobox (photo threads in rooms, thumbnails, lightbox, permission system)
- Feedback Batch 1: 13 fixes (V041, room/admin/member improvements, seed data)
- Multi-Role Support: Users can have multiple assigned roles (TEACHER/PARENT/SECTION_ADMIN) and switch at runtime (V042)
- Post-batch features: Putzaktion rename, specific date cleaning configs (V043), Bundesland/holiday config, calendar→feed integration, targeted feed posts (V044)
- Audience visibility: Folders and fotobox threads with role-based visibility (V045)
- Multi-section forms: Forms can target multiple school sections, dashboard widget for pending forms (V046)
- Annual billing (Jahresabrechnung): Family hour aggregation with billing periods (V047)
- Error reporting: Fingerprint-based dedup, admin status management, GitHub Issue integration (V048)
- Section Admin panel: Dedicated SECTION_ADMIN management views, fixed special roles format (V049)
- All modules enabled by default: Simplified initial setup (V050)
- Auto-folder creation: KLASSE rooms get default folders via RoomCreatedEvent cross-module event
- Notification delete endpoint: Users can delete individual notifications

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
