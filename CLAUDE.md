# CLAUDE.md — MonteWeb Schul-Intranet

## Projektübersicht

MonteWeb ist ein modulares, selbst-gehostetes Schul-Intranet für Montessori-Schulkomplexe (Krippe bis Oberstufe). Es verwaltet Räume (Klassen/Gruppen/Projekte), einen Feed mit Nachrichten, Direktkommunikation, eine Jobbörse für Elternstunden und eine Putz-Organisation mit QR-Check-in.

**GitHub:** https://github.com/phash/monteweb (privat)

---

## Projektstatus

Alle 18 Phasen sind abgeschlossen. Das Projekt kompiliert, alle 565 Frontend-Tests bestehen (79 Testdateien), und der volle Docker-Stack läuft.

### Kern-Phasen (1–6)

| Phase | Inhalt | Status |
|-------|--------|--------|
| 1 | Gerüst, Auth, User, Family, School, Room, Admin | COMPLETE |
| 2 | Feed, Notifications, Files, PWA | COMPLETE |
| 3 | Jobbörse (Elternstunden) | COMPLETE |
| 4 | Putz-Organisation | COMPLETE |
| 5 | Messaging & Chat | COMPLETE |
| 6 | i18n, Security-Hardening, DSGVO, Production-Setup | COMPLETE |

### Erweiterungs-Phasen (7–18)

| Phase | Inhalt | Status |
|-------|--------|--------|
| 7 | Messaging-Inbox mit User-Picker & Kommunikationsregeln | COMPLETE |
| 8 | Raum-Diskussions-Threads (LEADER-Berechtigungen) | COMPLETE |
| 9 | E-Mail-Versand (SMTP, conditional) | COMPLETE |
| 10 | Englische Übersetzung (i18n DE+EN) + Language-Switcher | COMPLETE |
| 11 | Testabdeckung (565 Frontend-Tests, 79 Testdateien + 37 Backend-Testdateien) | COMPLETE |
| 12 | CI/CD Pipeline (GitHub Actions) | COMPLETE |
| 13 | OIDC/SSO (OAuth2 Client, conditional) | COMPLETE |
| 14 | PDF-Export (Stundenbericht, QR-Codes) | COMPLETE |
| 15 | Web Push Notifications (VAPID) | COMPLETE |
| 16 | Monitoring (Prometheus + Grafana) | COMPLETE |
| 17 | Kalender/Events (Raum/Bereich/Schulweit, RSVP) | COMPLETE |
| 18 | Formulare & Umfragen (Survey/Consent, Scopes, Export) | COMPLETE |
| 19 | Fotobox (Foto-Threads in Räumen, Thumbnails, Lightbox) | COMPLETE |

---

## Tech-Stack

### Backend
- **Java 21** mit **Spring Boot 3.4.2**
- **Spring Modulith 1.3.2** für modulare Architektur
- **Spring Security** mit JWT + Redis Sessions
- **Spring OAuth2 Client** für OIDC/SSO (conditional via `monteweb.oidc.enabled`)
- **Spring Data JPA** (Hibernate, `ddl-auto: validate`) + **Flyway** (V001–V039)
- **Spring WebSocket** + Redis Pub/Sub für Echtzeit
- **Spring Boot Actuator** für Health/Info/Prometheus-Endpoints
- **Spring Boot Mail** für E-Mail-Versand (conditional via `monteweb.email.enabled`)
- **OpenHTMLToPDF** für PDF-Generierung (Stundenberichte, QR-Codes)
- **web-push** (VAPID) für Push Notifications (conditional via `monteweb.push.enabled`)
- **Micrometer + Prometheus** für Metriken
- **Testcontainers** (PostgreSQL + Redis) für Integration Tests
- **PostgreSQL 16** als Hauptdatenbank
- **Redis 7** für Cache, Sessions, Pub/Sub
- **MinIO** für Dateispeicher (S3-kompatibel)
- **Maven** als Build-Tool

### Frontend
- **Vue 3.5** mit **Composition API** + `<script setup>`
- **TypeScript 5.9** (strikt)
- **Vite 7.3** als Build-Tool
- **PrimeVue 4.5** mit Aura-Theme (+ PrimeIcons 7)
- **Pinia 3** für State Management
- **Vue Router 4.6** für Routing
- **vue-i18n 11** für Internationalisierung (Deutsch + Englisch)
- **Axios 1.13** als HTTP-Client
- **Vitest 4** + **@vue/test-utils** + **@vitest/coverage-v8** für Unit/Component-Tests (565 Tests, 79 Testdateien)
- **PWA** via vite-plugin-pwa

### Infrastruktur
- **Docker Compose** für Entwicklung (`docker-compose.dev.yml`) und Produktion (`docker-compose.yml`)
- **nginx** als Reverse Proxy + SPA-Server (Production)
- Multi-Stage Dockerfiles für Backend und Frontend
- **GitHub Actions** CI/CD Pipeline (`.github/workflows/ci.yml`)
- **Prometheus + Grafana** Monitoring (optional, via `docker compose --profile monitoring`)

---

## Projektstruktur

```
monteweb/
├── docker-compose.yml          # Production (5+ Services, optional monitoring profile)
├── docker-compose.dev.yml      # Dev (Postgres, Redis, MinIO)
├── .env.example
├── .github/workflows/ci.yml   # CI/CD: backend, frontend, docker jobs
├── docs/
│
├── backend/
│   ├── pom.xml
│   ├── Dockerfile              # Multi-Stage: maven → temurin-21-jre
│   └── src/
│       ├── main/java/com/monteweb/
│       │   ├── MonteWebApplication.java
│       │   │
│       │   ├── auth/               # Authentifizierung, JWT, Password-Reset, OIDC
│       │   │   ├── AuthModuleApi.java          # Public Facade
│       │   │   ├── AuthInfo.java               # Public DTO
│       │   │   └── internal/
│       │   │       ├── config/OidcConfig.java          # Conditional OAuth2 Client
│       │   │       ├── controller/OidcAuthController.java  # SSO endpoints
│       │   │       └── service/OidcUserService.java    # OIDC user resolution
│       │   │
│       │   ├── user/               # User-Management, Profil, Rollen, OIDC-Felder
│       │   │   ├── UserModuleApi.java          # inkl. OIDC + Search Methoden
│       │   │   ├── UserInfo.java
│       │   │   └── internal/
│       │   │
│       │   ├── family/             # Familienverbund, Einladungscodes, Stundenkonto, Einladungen
│       │   │   ├── FamilyModuleApi.java
│       │   │   ├── FamilyInfo.java
│       │   │   ├── FamilyInvitationEvent.java     # Spring Event
│       │   │   └── internal/
│       │   │       ├── model/FamilyInvitation.java, FamilyInvitationStatus.java
│       │   │       ├── repository/FamilyInvitationRepository.java
│       │   │       ├── dto/FamilyInvitationInfo.java, InviteMemberRequest.java
│       │   │
│       │   ├── school/             # Schulbereiche (Krippe, KiGa, GS, MS, OS)
│       │   │   ├── SchoolModuleApi.java
│       │   │   ├── SectionInfo.java
│       │   │   └── internal/
│       │   │
│       │   ├── room/               # Räume + Diskussions-Threads + Beitrittsanfragen
│       │   │   ├── RoomModuleApi.java
│       │   │   ├── RoomInfo.java
│       │   │   ├── DiscussionThreadCreatedEvent.java   # Spring Event
│       │   │   ├── RoomJoinRequestEvent.java           # Spring Event
│       │   │   ├── RoomJoinRequestResolvedEvent.java   # Spring Event
│       │   │   └── internal/
│       │   │       ├── model/DiscussionThread.java, DiscussionReply.java, ThreadStatus.java
│       │   │       ├── model/RoomJoinRequest.java, RoomJoinRequestStatus.java
│       │   │       ├── repository/DiscussionThreadRepository.java, DiscussionReplyRepository.java
│       │   │       ├── repository/RoomJoinRequestRepository.java
│       │   │       ├── dto/JoinRequestInfo.java
│       │   │       ├── service/DiscussionThreadService.java
│       │   │       └── controller/DiscussionThreadController.java
│       │   │
│       │   ├── feed/               # Unified Feed, Posts, Kommentare, Banner
│       │   │   ├── FeedModuleApi.java
│       │   │   ├── FeedPostInfo.java
│       │   │   └── internal/
│       │   │
│       │   ├── calendar/             # [Optional] Kalender & Events mit RSVP
│       │   │   ├── CalendarModuleApi.java       # Public Facade
│       │   │   ├── EventInfo.java               # Public DTO (inkl. RSVP-Counts)
│       │   │   ├── EventScope.java              # Enum: ROOM, SECTION, SCHOOL
│       │   │   ├── EventRecurrence.java         # Enum: NONE, DAILY, WEEKLY, MONTHLY, YEARLY
│       │   │   ├── RsvpStatus.java              # Enum: ATTENDING, MAYBE, DECLINED
│       │   │   ├── CreateEventRequest.java, UpdateEventRequest.java
│       │   │   ├── EventCreatedEvent.java, EventCancelledEvent.java  # Spring Events
│       │   │   └── internal/
│       │   │       ├── config/CalendarModuleConfig.java
│       │   │       ├── model/CalendarEvent.java, EventRsvp.java
│       │   │       ├── repository/CalendarEventRepository.java, EventRsvpRepository.java
│       │   │       ├── service/CalendarService.java
│       │   │       └── controller/CalendarController.java
│       │   │
│       │   ├── notification/       # In-App + Push Benachrichtigungen
│       │   │   ├── NotificationModuleApi.java
│       │   │   ├── NotificationType.java       # inkl. DISCUSSION_*, EVENT_*, ROOM_JOIN_*, FAMILY_INVITATION_*
│       │   │   └── internal/
│       │   │       ├── model/PushSubscription.java
│       │   │       ├── service/WebPushService.java             # VAPID Push
│       │   │       ├── service/DiscussionNotificationListener.java
│       │   │       ├── service/CalendarNotificationListener.java
│       │   │       ├── service/RoomJoinRequestNotificationListener.java
│       │   │       ├── service/FamilyInvitationNotificationListener.java
│       │   │       └── controller/PushNotificationController.java
│       │   │
│       │   ├── admin/              # System-Config, Audit-Log, Modul-Verwaltung
│       │   │   └── internal/
│       │   │
│       │   ├── messaging/          # [Optional] Direktnachrichten & Chat + Kommunikationsregeln
│       │   │   ├── MessagingModuleApi.java
│       │   │   └── internal/
│       │   │
│       │   ├── files/              # [Optional] Dateiablage via MinIO
│       │   │   └── internal/
│       │   │
│       │   ├── jobboard/           # [Optional] Jobbörse & Elternstunden + PDF-Export
│       │   │   ├── JobboardModuleApi.java
│       │   │   └── internal/
│       │   │
│       │   ├── cleaning/           # [Optional] Putz-Orga mit QR-Check-in + QR-PDF-Export
│       │   │   └── internal/
│       │   │
│       │   ├── forms/              # [Optional] Formulare & Umfragen (Survey/Consent) + CSV/PDF-Export
│       │   │   ├── FormsModuleApi.java         # Public Facade
│       │   │   ├── FormInfo.java, FormDetailInfo.java  # Public DTOs
│       │   │   ├── FormStatus.java             # Enum: DRAFT, PUBLISHED, CLOSED, ARCHIVED
│       │   │   ├── FormScope.java              # Enum: ROOM, SECTION, SCHOOL
│       │   │   ├── FormType.java               # Enum: SURVEY, CONSENT
│       │   │   ├── QuestionType.java           # Enum: TEXT, SINGLE_CHOICE, MULTIPLE_CHOICE, RATING, YES_NO
│       │   │   ├── FormPublishedEvent.java, FormClosedEvent.java  # Spring Events
│       │   │   └── internal/
│       │   │       ├── config/FormsModuleConfig.java
│       │   │       ├── model/Form.java, FormQuestion.java, FormResponse.java, FormAnswer.java
│       │   │       ├── repository/FormRepository.java, FormQuestionRepository.java, ...
│       │   │       ├── service/FormsService.java
│       │   │       └── controller/FormsController.java, FormsExportController.java
│       │   │
│       │   ├── fotobox/             # [Optional] Foto-Threads in Räumen (Galerie + Lightbox)
│       │   │   ├── FotoboxModuleApi.java        # Public Facade
│       │   │   ├── FotoboxThreadInfo.java, FotoboxImageInfo.java  # Public DTOs
│       │   │   ├── FotoboxPermissionLevel.java  # Enum: VIEW_ONLY, POST_IMAGES, CREATE_THREADS
│       │   │   ├── FotoboxThreadCreatedEvent.java  # Spring Event
│       │   │   └── internal/
│       │   │       ├── model/FotoboxThread.java, FotoboxImage.java, FotoboxRoomSettings.java
│       │   │       ├── repository/FotoboxThreadRepository.java, FotoboxImageRepository.java, FotoboxRoomSettingsRepository.java
│       │   │       ├── service/FotoboxService.java, FotoboxPermissionService.java, FotoboxStorageService.java
│       │   │       ├── controller/FotoboxController.java
│       │   │       └── dto/CreateThreadRequest.java, UpdateThreadRequest.java, UpdateSettingsRequest.java, ...
│       │   │
│       │   └── shared/             # Querschnittsthemen (kein Modul, via @NamedInterface exponiert)
│       │       ├── config/         # CORS, SecurityConfig, RateLimitFilter, WebSocketConfig, EmailProperties, EmailService
│       │       ├── dto/            # ApiResponse, ErrorResponse, PageResponse
│       │       ├── exception/      # GlobalExceptionHandler, Custom Exceptions
│       │       └── util/           # SecurityUtils, PdfService
│       │
│       └── test/java/com/monteweb/       # 37 Testdateien
│           ├── TestContainerConfig.java            # Shared Testcontainers (PostgreSQL + Redis)
│           ├── TestHelper.java                     # Hilfsmethoden (registerAndGetToken, parseResponse)
│           ├── auth/AuthControllerIntegrationTest.java, AuthServiceIntegrationTest.java
│           ├── user/UserServiceIntegrationTest.java, UserControllerIntegrationTest.java
│           ├── school/SchoolSectionServiceTest.java, SchoolSectionControllerIntegrationTest.java
│           ├── room/RoomControllerIntegrationTest.java, RoomServiceIntegrationTest.java
│           ├── room/DiscussionThreadControllerIntegrationTest.java, RoleConceptIntegrationTest.java
│           ├── family/FamilyControllerIntegrationTest.java, FamilyServiceIntegrationTest.java
│           ├── feed/FeedControllerIntegrationTest.java, FeedServiceIntegrationTest.java
│           ├── calendar/CalendarControllerIntegrationTest.java, CalendarServiceIntegrationTest.java
│           ├── messaging/MessagingControllerIntegrationTest.java, MessagingServiceIntegrationTest.java
│           ├── jobboard/JobboardControllerIntegrationTest.java, JobboardServiceIntegrationTest.java
│           ├── cleaning/CleaningControllerIntegrationTest.java, CleaningServiceIntegrationTest.java
│           ├── notification/NotificationServiceIntegrationTest.java, NotificationControllerIntegrationTest.java
│           ├── forms/FormsControllerIntegrationTest.java, FormsServiceIntegrationTest.java
│           ├── fotobox/FotoboxControllerIntegrationTest.java
│           ├── admin/AdminConfigControllerIntegrationTest.java, AdminModuleApiTest.java
│           └── shared/GlobalExceptionHandlerTest.java, ExceptionHandlerTest.java, SecurityUtilsTest.java, ...
│
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   ├── vitest.config.ts        # Test-Konfiguration (jsdom, v8-coverage, 55% threshold)
│   ├── tsconfig.json
│   ├── tsconfig.app.json       # Excludes test files
│   ├── nginx.conf              # SPA-Config für Docker-Runtime
│   ├── Dockerfile              # Multi-Stage: node:22 → nginx:alpine
│   └── src/
│       ├── main.ts
│       ├── App.vue
│       ├── test/setup.ts       # localStorage Mock für Vitest
│       ├── router/index.ts     # Lazy-loaded Routes + Auth-Guards
│       ├── i18n/
│       │   ├── index.ts        # Browser-Locale-Detection
│       │   ├── de.ts           # Deutsche Übersetzungen (~375+ Keys)
│       │   └── en.ts           # Englische Übersetzungen
│       ├── stores/             # Pinia Stores (auth, user, feed, rooms, family, discussions, calendar, forms, fotobox, ...)
│       │   ├── discussions.ts  # Diskussions-Threads Store
│       │   ├── calendar.ts     # Kalender/Events Store
│       │   ├── forms.ts        # Formulare/Umfragen Store
│       │   ├── fotobox.ts      # Fotobox Store (Threads, Images, Settings)
│       │   └── __tests__/      # Store Unit Tests
│       ├── api/                # Axios-Wrapper pro Modul (client.ts + *.api.ts)
│       │   ├── discussions.api.ts      # Diskussions-Thread API
│       │   ├── calendar.api.ts         # Kalender/Events API
│       │   ├── forms.api.ts            # Formulare/Umfragen API
│       │   └── fotobox.api.ts          # Fotobox API (Threads, Images, URL-Helpers mit JWT-Token)
│       ├── types/              # TypeScript Interfaces (user, room, feed, family, discussion, calendar, forms, fotobox, ...)
│       ├── composables/        # useAuth, useNotifications, useWebSocket, usePushNotifications
│       ├── components/
│       │   ├── common/         # PageTitle, LoadingSpinner, EmptyState, ErrorBoundary, LanguageSwitcher
│       │   │   └── __tests__/  # 8 Testdateien (27 Tests)
│       │   ├── layout/         # AppHeader (+ LanguageSwitcher), AppSidebar, BottomNav
│       │   │   └── __tests__/  # AppSidebar, BottomNav Tests
│       │   ├── feed/           # FeedList, FeedPost, PostComposer
│       │   │   └── __tests__/  # FeedPost, FeedList, SystemBanner Tests
│       │   ├── rooms/          # RoomFiles, RoomChat, RoomDiscussions, DiscussionThreadView, RoomEvents, RoomFotobox, FotoboxThread, FotoboxLightbox
│       │   │   └── __tests__/  # RoomCard, RoomChat, RoomFiles, DiscussionThreadView, RoomFotobox, FotoboxThread, FotoboxLightbox Tests
│       │   ├── messaging/      # NewMessageDialog (User-Picker)
│       │   │   └── __tests__/  # NewMessageDialog Tests
│       │   └── family/         # FamilyHoursWidget, InviteMemberDialog
│       │       └── __tests__/  # FamilyHoursWidget, InviteMemberDialog Tests
│       ├── views/
│       │   ├── __tests__/              # 19 View-Testdateien
│       │   ├── LoginView.vue           # + SSO-Button (conditional)
│       │   ├── DashboardView.vue       # = Feed-Ansicht
│       │   ├── RoomsView.vue
│       │   ├── RoomDetailView.vue      # Tabs: Info-Board, Members (+Join Requests), Discussions, Chat, Files, Events, Fotobox
│       │   ├── DiscoverRoomsView.vue   # + Browse alle Räume + Beitrittsanfragen
│       │   ├── MessagesView.vue        # + NewMessageDialog, Deep-Link via conversationId
│       │   ├── JobBoardView.vue
│       │   ├── JobCreateView.vue
│       │   ├── JobDetailView.vue
│       │   ├── CleaningView.vue
│       │   ├── CleaningSlotView.vue
│       │   ├── CalendarView.vue        # Agenda-Liste mit Monatsnavigation
│       │   ├── EventDetailView.vue     # Event-Info + RSVP + Aktionen
│       │   ├── EventCreateView.vue     # Erstellen/Bearbeiten mit Scope-Picker
│       │   ├── FormsView.vue           # Formulare-Liste
│       │   ├── FormCreateView.vue      # Formular erstellen/bearbeiten
│       │   ├── FormDetailView.vue      # Formular ausfüllen + Details
│       │   ├── FormResultsView.vue     # Ergebnis-Auswertung + Export
│       │   ├── FamilyView.vue          # + Einladungen senden/empfangen
│       │   ├── ProfileView.vue         # + Push Notification Toggle
│       │   ├── NotFoundView.vue        # 404
│       │   └── admin/
│       │       ├── __tests__/          # 6 Admin-View-Testdateien
│       │       ├── AdminDashboard.vue
│       │       ├── AdminUsers.vue
│       │       ├── AdminRooms.vue
│       │       ├── AdminSections.vue
│       │       ├── AdminModules.vue
│       │       ├── AdminTheme.vue
│       │       ├── AdminCleaning.vue   # + QR-PDF-Export
│       │       └── AdminJobReport.vue  # + PDF-Export
│       └── assets/styles/
│           ├── variables.css   # CSS Custom Properties (Theming)
│           └── global.css
│
├── monitoring/
│   ├── prometheus.yml                              # Scrape-Config
│   └── grafana/
│       ├── dashboards/monteweb-overview.json       # 7-Panel Dashboard
│       └── provisioning/
│           ├── datasources/prometheus.yml
│           └── dashboards/dashboards.yml
│
└── nginx/
    └── nginx.conf              # Production: Reverse Proxy + Security Headers
```

---

## Architektur-Regeln

### Backend (Spring Modulith)

1. **Modulstruktur:** Jedes Modul ist ein direktes Sub-Package von `com.monteweb` (NICHT verschachtelt unter `core/` oder `modules/`).
   - **Public API** (Root-Package): Facade-Interfaces (`*ModuleApi`), DTOs (`*Info` Records), Events, Enums
   - **Internal** (`internal/`): Service, Controller, Model, Repository — niemals von anderen Modulen direkt importieren!
   - Inter-Modul-Kommunikation: Facades für synchrone Aufrufe, Spring Events für asynchrone

2. **Shared-Package:** `com.monteweb.shared` ist kein Modul, sondern stellt Querschnittsthemen bereit. Sub-Packages sind via `@NamedInterface` in `package-info.java` exponiert (`shared-dto`, `shared-exception`, `shared-util`, `shared-config`).

3. **API-Design:** RESTful unter `/api/v1/`. Antworten als `ApiResponse<T>`:
   ```java
   public record ApiResponse<T>(T data, String message, boolean success) {}
   ```

4. **Optionale Module:** Werden über `@ConditionalOnProperty(prefix = "monteweb.modules", name = "xyz.enabled")` aktiviert. ALLE Beans (Services, Controllers, Components) brauchen diese Annotation, nicht nur Config-Klassen.

5. **Flyway-Migrationen:** V001–V039 vorhanden. Jede Schema-Änderung als neue Migration. Hibernate validiert nur (`ddl-auto: validate`).

6. **Security:** Alle Endpunkte gesichert. JWT (15min Access + 7d Refresh). Rate-Limiting auf Auth-Endpoints. Security-Headers (HSTS, X-Frame-Options, CSP). Fotobox-Bild-Endpoints akzeptieren JWT auch via `?token=` Query-Parameter (da `<img>`-Tags keine Authorization-Header senden können).

### Frontend

1. **Composition API + `<script setup>`:** Kein Options API. TypeScript in allen Dateien.

2. **PrimeVue 4:** Aura-Theme. Komponenten einzeln importieren (kein globales Plugin). `ToastService` ist global registriert (`main.ts`), `<Toast />` liegt in `App.vue`. Views nutzen `useToast()` für Feedback.

3. **Pinia Stores:** Ein Store pro Domäne. Stores rufen API-Funktionen auf, halten den State.

4. **i18n:** Alle UI-Texte als Keys via `vue-i18n`. Sprachen: `src/i18n/de.ts` (Deutsch) + `src/i18n/en.ts` (Englisch). Browser-Locale-Detection in `src/i18n/index.ts`. LanguageSwitcher im Header. Keine hardcodierten Strings in `.vue`-Dateien.

5. **API-Client:** Zentraler Axios-Client (`api/client.ts`) mit JWT-Interceptor (auto-refresh), Error-Handling.

6. **Routing:** Lazy-loaded Routes. Auth-Guards. Catch-all 404-Route.

7. **Responsive:** Bottom Navigation auf Mobile, Sidebar auf Desktop.

8. **Theming:** CSS Custom Properties (`--mw-*`). Theme wird vom Backend geladen und als CSS-Variablen gesetzt.

---

## Datenbank-Migrationen

```
V001  tenant_config
V002  users
V003  school_sections
V004  families + family_members
V005  rooms + room_members
V006  audit_log
V007  seed_default_tenant
V008  feed_posts + feed_post_comments
V009  notifications
V010  conversations + messages
V011  files + file_folders
V012  event_publication (Spring Modulith)
V013  jobs
V014  job_assignments
V015  cleaning_configs
V016  cleaning_slots + cleaning_registrations
V017  room_interest_fields
V018  room_chat_channels
V019  user_deletion_fields (DSGVO)
V020  password_reset_tokens
V021  communication_rules (tenant_config: parent_to_parent, student_to_student messaging)
V022  discussion_threads + discussion_replies
V023  push_subscriptions
V024  OIDC-Felder (users: oidc_provider, oidc_subject, password_hash nullable)
V025  calendar_events + calendar_event_rsvps
V026  calendar-Modul zu default-modules
V027  job_event_link (Jobs mit Kalender-Events verknüpfen)
V028  create_forms (Formulare, Fragen, Antworten, Tracking)
V029  add_forms_to_default_modules (Forms-Modul Standardkonfiguration)
V030  add_target_cleaning_hours (Konfigurierbares Putzstunden-Ziel)
V031  add_avatars_and_public_description (Avatare, öffentliche Raumbeschreibungen)
V032  seed_default_admin (Standard-Superadmin-Account)
V033  seed_test_users (Testdaten für alle Rollen)
V034  room_join_requests (Beitrittsanfragen für Räume)
V035  family_invitations (Familien-Einladungen per User-Suche)
V036  add_thread_audience (Zielgruppen-Sichtbarkeit für Diskussions-Threads)
V037  role_concept_refactoring
V038  create_fotobox_tables (fotobox_room_settings, fotobox_threads, fotobox_images)
V039  fix_fotobox_schema (COALESCE modules NULL, ON DELETE SET NULL für DSGVO)
```

---

## API-Endpunkte (Überblick)

### Auth (`/api/v1/auth`)
```
POST   /register              Registrierung
POST   /login                 Login (JWT)
POST   /logout                Logout
POST   /refresh               Token Refresh
POST   /password-reset        Passwort-Reset anfordern
POST   /password-reset/confirm  Passwort-Reset bestätigen
GET    /oidc/config            OIDC-Verfügbarkeit + Authorization-URI
POST   /oidc/token             OIDC-Claims → JWT-Token-Austausch
```

### Users (`/api/v1/users`)
```
GET    /me                    Eigenes Profil
PUT    /me                    Profil aktualisieren
POST   /me/avatar             Avatar hochladen
DELETE /me/avatar             Avatar löschen
GET    /me/data-export        DSGVO: Alle eigenen Daten als JSON
DELETE /me                    DSGVO: Account anonymisieren/löschen
GET    /{id}                  User-Profil (eingeschränkt)
GET    /search?q={query}      User-Suche (für Messaging User-Picker)
```

### Admin Users (`/api/v1/admin/users`) [SUPERADMIN]
```
GET    /                      User-Liste (paginiert)
PUT    /{id}/profile          Profil bearbeiten
PUT    /{id}/roles            Rollen ändern
PUT    /{id}/status           Status ändern (aktiv/inaktiv)
GET    /{id}/rooms            Raum-Mitgliedschaften
GET    /{id}/families         Familien-Mitgliedschaften
POST   /{id}/families/{familyId}     User zu Familie hinzufügen
DELETE /{id}/families/{familyId}     User aus Familie entfernen
GET    /search-special        Suche nach Sonderrollen
POST   /{id}/special-roles    Sonderrolle zuweisen
DELETE /{id}/special-roles/{role}  Sonderrolle entfernen
```

### Families (`/api/v1/families`)
```
POST   /                      Familienverbund erstellen
GET    /mine                   Eigenen Verbund abrufen
POST   /join                   Per Einladungscode beitreten
POST   /{id}/invite            Einladungscode generieren
POST   /{id}/children          Kind verknüpfen
DELETE /{id}/members/{userId}  Mitglied entfernen
GET    /{id}/hours             Stundenkonto
POST   /{id}/invitations       Mitglied per User-Suche einladen
GET    /{id}/invitations       Offene Einladungen einer Familie
GET    /my-invitations         Meine empfangenen Einladungen
POST   /invitations/{id}/accept   Einladung annehmen
POST   /invitations/{id}/decline  Einladung ablehnen
```

### School Sections (`/api/v1/sections`)
```
GET    /                       Alle Schulbereiche
POST   /                       Anlegen (Admin)
PUT    /{id}                   Bearbeiten
DELETE /{id}                   Deaktivieren
```

### Rooms (`/api/v1/rooms`)
```
GET    /mine                   Meine Räume
GET    /                       Alle sichtbaren Räume
GET    /browse?q=&page=&size=  Alle Räume wo User nicht Mitglied (für Beitrittsanfragen)
GET    /discover               Räume entdecken (öffentliche Räume)
POST   /                       Raum erstellen
POST   /interest               Interessensraum erstellen
GET    /{id}                   Raum-Details
PUT    /{id}                   Raum bearbeiten
PUT    /{id}/settings          Raum-Einstellungen
PUT    /{id}/interest          Interessensraum-Felder bearbeiten
POST   /{id}/avatar            Raum-Avatar hochladen
DELETE /{id}/avatar            Raum-Avatar löschen
PUT    /{id}/archive           Raum archivieren (Admin)
DELETE /{id}                   Raum löschen (Admin)
POST   /{id}/join              Direkt beitreten (bei OPEN JoinPolicy)
POST   /{id}/leave             Raum verlassen
POST   /{id}/members           Mitglied hinzufügen
DELETE /{id}/members/{userId}  Mitglied entfernen
PUT    /{id}/members/{userId}/role  Mitglied-Rolle ändern
POST   /{id}/families/{familyId}   Familien-Kinder zum Raum hinzufügen
POST   /{id}/mute              Raum im Feed stumm schalten
POST   /{id}/unmute            Stummschaltung aufheben
POST   /{id}/join-request      Beitrittsanfrage senden (body: { message?: string })
GET    /{id}/join-requests     Offene Anfragen (Leader only)
POST   /{id}/join-requests/{rid}/approve  Anfrage annehmen (Leader)
POST   /{id}/join-requests/{rid}/deny     Anfrage ablehnen (Leader)
GET    /my-join-requests       Eigene Beitrittsanfragen
```

### Room Chat (`/api/v1/rooms/{roomId}/chat`)
```
GET    /channels               Chat-Kanäle auflisten (MAIN, PARENTS, STUDENTS)
POST   /channels               Chat-Kanal erstellen (Leader)
```

### Discussion Threads (`/api/v1/rooms/{roomId}/threads`)
```
GET    /                       Threads auflisten (paginiert)
POST   /                       Thread erstellen (LEADER)
GET    /{threadId}             Thread-Detail
PUT    /{threadId}/archive     Archivieren (LEADER)
DELETE /{threadId}             Löschen (LEADER)
GET    /{threadId}/replies     Antworten (paginiert)
POST   /{threadId}/replies     Antwort hinzufügen (nur wenn ACTIVE)
```

### Feed (`/api/v1/feed`)
```
GET    /                       Persönlicher Feed (paginiert)
GET    /banners                Aktive System-Banner
POST   /posts                  Post erstellen
GET    /posts/{id}             Post-Detail
PUT    /posts/{id}             Post bearbeiten
DELETE /posts/{id}             Post löschen
POST   /posts/{id}/pin         Post anheften
POST   /posts/{id}/comments    Kommentar hinzufügen
```

### Messaging (`/api/v1/messages`) [Modul: messaging]
```
GET    /conversations          Meine Konversationen
POST   /conversations          Neue Konversation
GET    /conversations/{id}/messages  Nachrichten laden
POST   /conversations/{id}/messages  Nachricht senden
WS     /ws/messages            WebSocket Echtzeit
```

### Files (`/api/v1/rooms/{roomId}/files`) [Modul: files]
```
GET    /                       Dateiliste
POST   /                       Datei hochladen
GET    /{fileId}               Datei herunterladen
DELETE /{fileId}               Datei löschen
POST   /folders                Ordner erstellen
```

### Jobboard (`/api/v1/jobs`) [Modul: jobboard]
```
GET    /                       Offene Jobs (paginiert, filterbar)
GET    /mine                   Meine erstellten Jobs
GET    /my-assignments         Meine Zuweisungen
GET    /categories             Job-Kategorien
GET    /by-event/{eventId}     Jobs zu einem Kalender-Event
POST   /                       Job erstellen
GET    /{id}                   Job-Detail
PUT    /{id}                   Job bearbeiten
DELETE /{id}                   Job löschen
PUT    /{id}/link-event        Job mit Kalender-Event verknüpfen
POST   /{id}/apply             Für Job anmelden
GET    /{id}/assignments       Zuweisungen zu einem Job
PUT    /assignments/{aid}/start     Zuweisung starten
PUT    /assignments/{aid}/complete  Zuweisung abschließen
PUT    /assignments/{aid}/confirm   Stunden bestätigen
DELETE /assignments/{aid}           Zuweisung abbrechen
GET    /family/{familyId}/hours     Familien-Stundenkonto
GET    /report                 Admin: Stunden-Übersicht
GET    /report/summary         Admin: Zusammenfassung
GET    /report/export          Admin: CSV-Export
GET    /report/pdf             Admin: PDF-Export
```

### Cleaning (`/api/v1/cleaning`) [Modul: cleaning]
```
GET    /slots                  Offene Putztermine
GET    /slots/mine             Meine Termine
GET    /slots/{id}             Slot-Detail
POST   /slots/{id}/register   Anmelden (Opt-in)
DELETE /slots/{id}/register    Abmelden
POST   /slots/{id}/swap        Tausch anbieten
GET    /slots/{id}/swaps       Verfügbare Tausch-Angebote
POST   /slots/{id}/checkin     QR-Check-in
POST   /slots/{id}/checkout    Check-out
GET    /configs                Putz-Konfigurationen (Admin/PUTZORGA/ELTERNBEIRAT)
POST   /configs                Konfiguration anlegen
PUT    /configs/{id}           Konfiguration bearbeiten
POST   /configs/{id}/generate  Termine generieren
GET    /configs/{id}/qr-codes  QR-Codes als PDF
PUT    /slots/{id}             Slot bearbeiten (Admin)
DELETE /slots/{id}             Slot löschen (Admin)
GET    /slots/{id}/qr          QR-Code für einzelnen Slot
GET    /dashboard              Admin-Dashboard
```

### Forms (`/api/v1/forms`) [Modul: forms]
```
GET    /                       Verfügbare Formulare (paginiert)
GET    /mine                   Eigene Formulare
POST   /                       Formular erstellen
GET    /{id}                   Formular-Detail mit Fragen
PUT    /{id}                   Formular bearbeiten (nur DRAFT)
DELETE /{id}                   Formular löschen (nur DRAFT)
POST   /{id}/publish           Formular veröffentlichen
POST   /{id}/close             Formular schließen
POST   /{id}/respond           Antwort einreichen
GET    /{id}/results            Aggregierte Ergebnisse (Ersteller/Admin)
GET    /{id}/responses          Einzelantworten (nicht-anonym, Ersteller/Admin)
GET    /{id}/results/csv        CSV-Export
GET    /{id}/results/pdf        PDF-Export
```

### Calendar (`/api/v1/calendar`) [Modul: calendar]
```
GET    /events?from=&to=       Persönlicher Kalender (paginiert)
POST   /events                 Event erstellen
GET    /events/{id}            Event-Detail
PUT    /events/{id}            Event bearbeiten
DELETE /events/{id}            Event löschen
POST   /events/{id}/cancel     Event absagen
POST   /events/{id}/rsvp       RSVP (ATTENDING/MAYBE/DECLINED)
GET    /rooms/{roomId}/events  Raum-Events
```

### Fotobox (`/api/v1/rooms/{roomId}/fotobox` + `/api/v1/fotobox`) [Modul: fotobox]
```
GET    /rooms/{roomId}/fotobox/settings                   Raum-Fotobox-Einstellungen
PUT    /rooms/{roomId}/fotobox/settings                   Einstellungen ändern (Leader/Admin)
GET    /rooms/{roomId}/fotobox/threads                    Alle Foto-Threads im Raum
POST   /rooms/{roomId}/fotobox/threads                    Thread erstellen (CREATE_THREADS)
GET    /rooms/{roomId}/fotobox/threads/{threadId}         Thread-Detail
PUT    /rooms/{roomId}/fotobox/threads/{threadId}         Thread bearbeiten (Owner/Leader)
DELETE /rooms/{roomId}/fotobox/threads/{threadId}         Thread löschen (Owner/Leader)
GET    /rooms/{roomId}/fotobox/threads/{threadId}/images  Bilder im Thread
POST   /rooms/{roomId}/fotobox/threads/{threadId}/images  Bilder hochladen (POST_IMAGES, max 20/Request)
PUT    /fotobox/images/{imageId}                          Bild bearbeiten (Caption, Sort)
DELETE /fotobox/images/{imageId}                          Bild löschen (Owner/Leader)
GET    /fotobox/images/{imageId}                          Bild herunterladen (+ ?token= JWT)
GET    /fotobox/images/{imageId}/thumbnail                Thumbnail herunterladen (+ ?token= JWT)
```

### Notifications (`/api/v1/notifications`)
```
GET    /                       Meine Benachrichtigungen (paginiert)
GET    /unread-count           Anzahl ungelesener Benachrichtigungen
PUT    /{id}/read              Als gelesen markieren
PUT    /read-all               Alle als gelesen
WS     /ws/notifications       WebSocket Push
```

### Push Notifications (`/api/v1/notifications/push`)
```
GET    /public-key             VAPID Public Key
POST   /subscribe              Push-Subscription registrieren
POST   /unsubscribe            Push-Subscription entfernen
```

### Public Config (`/api/v1/config`)
```
GET    /                       Öffentliche Konfiguration (Theme, Module, Schulname)
```

### Admin (`/api/v1/admin`) [SUPERADMIN]
```
GET    /config                 Systemkonfiguration (vollständig)
PUT    /config                 Konfiguration aktualisieren
PUT    /config/theme           Theme-Einstellungen
PUT    /config/modules         Module aktivieren/deaktivieren
POST   /config/logo            Logo hochladen
GET    /audit-log              Audit-Log
```

### Actuator
```
GET    /actuator/health        Health-Check (DB, Redis)
GET    /actuator/info          App-Info
GET    /actuator/prometheus    Prometheus Metriken
GET    /actuator/metrics       Micrometer Metriken
```

---

## Wichtige fachliche Regeln

1. **Familienverbund ist die Abrechnungseinheit.** Stunden aus Jobbörse und Putz-Orga werden dem Familienverbund gutgeschrieben, nicht der Einzelperson. Putzstunden haben ein Sonder-Unterkonto.

2. **Ein Elternteil gehört zu genau einem Familienverbund.** Ein Kind kann mehreren Verbünden zugeordnet sein (getrennte Eltern).

3. **Putz-Orga ist Opt-in, nicht Rotation.** Eltern melden sich freiwillig für Termine an.

4. **Feed-Banner werden kontextabhängig angezeigt:** Putz-Banner nur für Eltern der betroffenen Schuleinheit.

5. **Raum-Posts sind Feed-Einträge.** Posts in einem Raum erscheinen im Feed aller Mitglieder (rollenabhängig).

6. **Module sind abschaltbar.** `@ConditionalOnProperty(prefix = "monteweb.modules", name = "xyz.enabled")` im Backend. Im Frontend: Menüpunkte nur sichtbar, wenn Modul aktiv (via `/api/v1/admin/config`).

7. **Kommunikationsregeln:** Lehrer-Eltern immer erlaubt. Eltern-Eltern und Schüler-Schüler standardmäßig deaktiviert, konfigurierbar.

8. **Kalender-Berechtigungen:** ROOM-Events: nur LEADER oder SUPERADMIN. SECTION-Events: TEACHER oder SUPERADMIN. SCHOOL-Events: nur SUPERADMIN. RSVP steht allen authentifizierten Nutzern offen.

9. **Raum-Beitrittsanfragen:** Nicht-Mitglieder können Beitrittsanfragen an geschlossene Räume senden. LEADERs erhalten Notifications und können Anfragen genehmigen/ablehnen. Genehmigte Anfragen fügen den User automatisch als MEMBER hinzu.

10. **Familien-Einladungen:** Familienmitglieder können andere User per AutoComplete-Suche einladen (mit Rollenwahl PARENT/CHILD). Eingeladene erhalten eine Notification und können annehmen oder ablehnen.

11. **Fotobox-Berechtigungen:** Dreistufig: VIEW_ONLY < POST_IMAGES < CREATE_THREADS. LEADER und SUPERADMIN haben immer CREATE_THREADS. Standard-Berechtigung pro Raum konfigurierbar. Bilder werden via MinIO gespeichert, Thumbnails automatisch generiert. Content-Type wird aus Magic Bytes erkannt (nicht aus HTTP-Header). Max 20 Dateien pro Upload-Request.

---

## Konventionen

### Allgemein
- Code-Sprache: **Englisch** (Variablen, Klassen, Kommentare)
- UI-Texte: **Deutsch + Englisch** (via i18n-Keys in `src/i18n/de.ts` + `en.ts`)
- Git: Conventional Commits (`feat:`, `fix:`, `chore:`)

### Java / Spring Boot
- Package: `com.monteweb.{modul}` (Public API) + `com.monteweb.{modul}.internal` (Implementierung)
- Records für DTOs: `public record CreateRoomRequest(String name, RoomType type) {}`
- Public DTOs: `*Info` Records (z.B. `UserInfo`, `RoomInfo`)
- Facades: `*ModuleApi` Interfaces (z.B. `UserModuleApi`)
- Entities: JPA `@Entity` mit Lombok `@Getter @Setter @NoArgsConstructor`
- UUIDs als Primary Keys, `Instant` für Timestamps (UTC)
- Bean Validation (`@NotNull`, `@Size`) auf Request-DTOs

### Vue / TypeScript
- `<script setup lang="ts">` in allen Komponenten
- PascalCase für Komponenten (`FeedPost.vue`)
- `use`-Prefix für Composables (`useAuth.ts`)
- Typen in `types/` Verzeichnis
- Scoped Styles, globale Variablen in `variables.css` (`--mw-*` Prefix)

### API
- Alle Endpunkte unter `/api/v1/`
- Paginierung: `?page=0&size=20&sort=createdAt,desc`
- Datumsformat: ISO 8601

---

## Entwicklung starten

**Voraussetzung:** Docker + Node.js 22+. Java ist NICHT lokal nötig (Backend wird via Docker kompiliert).

```bash
# 1. Infrastruktur starten (Postgres:5433, Redis:6380, MinIO:9000)
docker compose -f docker-compose.dev.yml up -d

# 2. Backend kompilieren und starten (lokal mit Maven oder via Docker)
cd backend && DB_PORT=5433 REDIS_PORT=6380 mvn spring-boot:run
# Alternativ via Docker:
# docker compose up backend

# 3. Frontend starten
cd frontend
npm install
npm run dev
```

**Port-Belegung (Dev):**
| Service | Host-Port | Container-Port |
|---------|-----------|----------------|
| PostgreSQL | 5433 | 5432 |
| Redis | 6380 | 6379 |
| MinIO API | 9000 | 9000 |
| MinIO Console | 9001 | 9001 |
| Backend | 8090 | 8080 |
| Frontend (dev) | 5173 | - |
| Frontend (Docker) | 8091 | 80 |

**Production-Stack:**
```bash
docker compose up -d   # Startet alle 5 Services (postgres, redis, minio, backend, frontend)
```

**Monitoring (optional):**
```bash
docker compose --profile monitoring up -d   # + Prometheus + Grafana
# Grafana: http://localhost:3000 (admin/admin)
# Prometheus: http://localhost:9090
```

**Tests:**
```bash
# Frontend (565 Tests, 79 Testdateien) — erfordert Node.js 22+
cd frontend && npm test
cd frontend && npm run test:coverage   # Mit Coverage-Report

# Backend (Testcontainers, Docker required, 37 Testdateien)
cd backend && mvn test
```

---

## Abgeschlossene Erweiterungen

- [x] E-Mail-Versand (SMTP, conditional via `monteweb.email.enabled`)
- [x] Englische Übersetzung (`de.ts` + `en.ts`, LanguageSwitcher, Browser-Locale-Detection)
- [x] Testabdeckung (565 Frontend-Tests in 79 Testdateien via Vitest; 37 Backend-Testdateien via Testcontainers)
- [x] CI/CD Pipeline (`.github/workflows/ci.yml` — Backend, Frontend, Docker Jobs)
- [x] OIDC/SSO (OAuth2 Client, conditional via `monteweb.oidc.enabled`, SSO-Button in Login)
- [x] PDF-Export (Stundenbericht + QR-Codes via OpenHTMLToPDF)
- [x] Push Notifications (VAPID Web Push, conditional via `monteweb.push.enabled`)
- [x] Monitoring (Prometheus + Grafana, Docker Compose Profile `monitoring`)
- [x] Messaging User-Picker (NewMessageDialog mit AutoComplete-Suche)
- [x] Kommunikationsregeln (Eltern-Eltern, Schüler-Schüler konfigurierbar)
- [x] Raum-Diskussions-Threads (LEADER erstellt/archiviert/löscht, Mitglieder antworten)
- [x] Kalender/Events (Raum/Bereich/Schulweit, RSVP, Monatsnavigation, conditional via `monteweb.modules.calendar.enabled`)
- [x] Raum-Beitrittsanfragen (Browse alle Räume, Anfrage senden, Leader genehmigt/lehnt ab, Notifications)
- [x] Familien-Einladungen per User-Suche (AutoComplete, Rolle wählen, Annehmen/Ablehnen, Notifications)
- [x] Formulare & Umfragen (Survey/Consent, Scopes, anonyme/nicht-anonyme Antworten, CSV/PDF-Export)
- [x] Fotobox (Foto-Threads in Räumen, Thumbnails via MinIO, Lightbox, Permission-System, JWT-Query-Param für Bilder)

## Conditional Features

Folgende Features sind über Konfiguration aktivierbar:

| Feature | Config-Property | Standard |
|---------|----------------|----------|
| E-Mail | `monteweb.email.enabled` | `false` |
| OIDC/SSO | `monteweb.oidc.enabled` | `false` |
| Push Notifications | `monteweb.push.enabled` | `false` |
| Messaging-Modul | `monteweb.modules.messaging.enabled` | `true` |
| Files-Modul | `monteweb.modules.files.enabled` | `true` |
| Jobboard-Modul | `monteweb.modules.jobboard.enabled` | `true` |
| Cleaning-Modul | `monteweb.modules.cleaning.enabled` | `true` |
| Calendar-Modul | `monteweb.modules.calendar.enabled` | `true` |
| Forms-Modul | `monteweb.modules.forms.enabled` | `true` |
| Fotobox-Modul | `monteweb.modules.fotobox.enabled` | `true` |
