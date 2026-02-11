# CLAUDE.md — MonteWeb Schul-Intranet

## Projektübersicht

MonteWeb ist ein modulares, selbst-gehostetes Schul-Intranet für Montessori-Schulkomplexe (Krippe bis Oberstufe). Es verwaltet Räume (Klassen/Gruppen/Projekte), einen Feed mit Nachrichten, Direktkommunikation, eine Jobbörse für Elternstunden und eine Putz-Organisation mit QR-Check-in.

**GitHub:** https://github.com/phash/monteweb (privat)

---

## Projektstatus

Alle 17 Phasen sind abgeschlossen. Das Projekt kompiliert, alle 87 Frontend-Tests bestehen, und der volle Docker-Stack läuft.

### Kern-Phasen (1–6)

| Phase | Inhalt | Status |
|-------|--------|--------|
| 1 | Gerüst, Auth, User, Family, School, Room, Admin | COMPLETE |
| 2 | Feed, Notifications, Files, PWA | COMPLETE |
| 3 | Jobbörse (Elternstunden) | COMPLETE |
| 4 | Putz-Organisation | COMPLETE |
| 5 | Messaging & Chat | COMPLETE |
| 6 | i18n, Security-Hardening, DSGVO, Production-Setup | COMPLETE |

### Erweiterungs-Phasen (7–17)

| Phase | Inhalt | Status |
|-------|--------|--------|
| 7 | Messaging-Inbox mit User-Picker & Kommunikationsregeln | COMPLETE |
| 8 | Raum-Diskussions-Threads (LEADER-Berechtigungen) | COMPLETE |
| 9 | E-Mail-Versand (SMTP, conditional) | COMPLETE |
| 10 | Englische Übersetzung (i18n DE+EN) + Language-Switcher | COMPLETE |
| 11 | Testabdeckung (Vitest 27 Tests + Backend Integration Tests) | COMPLETE |
| 12 | CI/CD Pipeline (GitHub Actions) | COMPLETE |
| 13 | OIDC/SSO (OAuth2 Client, conditional) | COMPLETE |
| 14 | PDF-Export (Stundenbericht, QR-Codes) | COMPLETE |
| 15 | Web Push Notifications (VAPID) | COMPLETE |
| 16 | Monitoring (Prometheus + Grafana) | COMPLETE |
| 17 | Kalender/Events (Raum/Bereich/Schulweit, RSVP) | COMPLETE |

---

## Tech-Stack

### Backend
- **Java 21** mit **Spring Boot 3.4.2**
- **Spring Modulith 1.3.2** für modulare Architektur
- **Spring Security** mit JWT + Redis Sessions
- **Spring OAuth2 Client** für OIDC/SSO (conditional via `monteweb.oidc.enabled`)
- **Spring Data JPA** (Hibernate, `ddl-auto: validate`) + **Flyway** (V001–V026)
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
- **Vitest 4** + **@vue/test-utils** für Unit/Component-Tests (87 Tests)
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
│       │   ├── family/             # Familienverbund, Einladungscodes, Stundenkonto
│       │   │   ├── FamilyModuleApi.java
│       │   │   ├── FamilyInfo.java
│       │   │   └── internal/
│       │   │
│       │   ├── school/             # Schulbereiche (Krippe, KiGa, GS, MS, OS)
│       │   │   ├── SchoolModuleApi.java
│       │   │   ├── SectionInfo.java
│       │   │   └── internal/
│       │   │
│       │   ├── room/               # Räume + Diskussions-Threads
│       │   │   ├── RoomModuleApi.java
│       │   │   ├── RoomInfo.java
│       │   │   ├── DiscussionThreadCreatedEvent.java   # Spring Event
│       │   │   └── internal/
│       │   │       ├── model/DiscussionThread.java, DiscussionReply.java, ThreadStatus.java
│       │   │       ├── repository/DiscussionThreadRepository.java, DiscussionReplyRepository.java
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
│       │   │   ├── NotificationType.java       # inkl. DISCUSSION_*, EVENT_*
│       │   │   └── internal/
│       │   │       ├── model/PushSubscription.java
│       │   │       ├── service/WebPushService.java             # VAPID Push
│       │   │       ├── service/DiscussionNotificationListener.java
│       │   │       ├── service/CalendarNotificationListener.java
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
│       │   └── shared/             # Querschnittsthemen (kein Modul, via @NamedInterface exponiert)
│       │       ├── config/         # CORS, SecurityConfig, RateLimitFilter, WebSocketConfig, EmailProperties, EmailService
│       │       ├── dto/            # ApiResponse, ErrorResponse, PageResponse
│       │       ├── exception/      # GlobalExceptionHandler, Custom Exceptions
│       │       └── util/           # SecurityUtils, PdfService
│       │
│       └── test/java/com/monteweb/
│           ├── TestContainerConfig.java            # Shared Testcontainers (PostgreSQL + Redis)
│           ├── auth/AuthControllerIntegrationTest.java
│           └── user/UserServiceIntegrationTest.java
│
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   ├── vitest.config.ts        # Test-Konfiguration (jsdom, setup file)
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
│       │   ├── de.ts           # Deutsche Übersetzungen (~350+ Keys)
│       │   └── en.ts           # Englische Übersetzungen
│       ├── stores/             # Pinia Stores (auth, user, feed, rooms, family, discussions, calendar, ...)
│       │   ├── discussions.ts  # Diskussions-Threads Store
│       │   ├── calendar.ts     # Kalender/Events Store
│       │   └── __tests__/      # Store Unit Tests (87 Tests)
│       │       ├── auth.test.ts        # 5 Tests
│       │       ├── messaging.test.ts   # 8 Tests
│       │       ├── feed.test.ts        # 9 Tests
│       │       └── calendar.test.ts    # 9 Tests
│       ├── api/                # Axios-Wrapper pro Modul (client.ts + *.api.ts)
│       │   ├── discussions.api.ts      # Diskussions-Thread API
│       │   └── calendar.api.ts         # Kalender/Events API
│       ├── types/              # TypeScript Interfaces (user, room, feed, family, discussion, calendar, ...)
│       ├── composables/        # useAuth, useNotifications, useWebSocket, usePushNotifications
│       ├── components/
│       │   ├── common/         # PageTitle, LoadingSpinner, EmptyState, ErrorBoundary, LanguageSwitcher
│       │   ├── layout/         # AppHeader (+ LanguageSwitcher), AppSidebar, BottomNav
│       │   ├── feed/           # FeedList, FeedPost, PostComposer
│       │   ├── rooms/          # RoomFiles, RoomChat, RoomDiscussions, DiscussionThreadView, RoomEvents
│       │   ├── messaging/      # NewMessageDialog (User-Picker)
│       │   ├── family/         # FamilyHoursWidget
│       │   └── __tests__/      # Component Tests
│       │       └── NewMessageDialog.test.ts  # 5 Tests
│       ├── views/
│       │   ├── LoginView.vue           # + SSO-Button (conditional)
│       │   ├── DashboardView.vue       # = Feed-Ansicht
│       │   ├── RoomsView.vue
│       │   ├── RoomDetailView.vue      # Tabs: Info-Board, Members, Discussions, Chat, Files, Events
│       │   ├── DiscoverRoomsView.vue
│       │   ├── MessagesView.vue        # + NewMessageDialog, Deep-Link via conversationId
│       │   ├── JobBoardView.vue
│       │   ├── JobCreateView.vue
│       │   ├── JobDetailView.vue
│       │   ├── CleaningView.vue
│       │   ├── CleaningSlotView.vue
│       │   ├── CalendarView.vue        # Agenda-Liste mit Monatsnavigation
│       │   ├── EventDetailView.vue     # Event-Info + RSVP + Aktionen
│       │   ├── EventCreateView.vue     # Erstellen/Bearbeiten mit Scope-Picker
│       │   ├── FamilyView.vue
│       │   ├── ProfileView.vue         # + Push Notification Toggle
│       │   ├── NotFoundView.vue        # 404
│       │   └── admin/
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

5. **Flyway-Migrationen:** V001–V026 vorhanden. Jede Schema-Änderung als neue Migration. Hibernate validiert nur (`ddl-auto: validate`).

6. **Security:** Alle Endpunkte gesichert. JWT (15min Access + 7d Refresh). Rate-Limiting auf Auth-Endpoints. Security-Headers (HSTS, X-Frame-Options, CSP).

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
GET    /me/data-export        DSGVO: Alle eigenen Daten als JSON
DELETE /me                    DSGVO: Account anonymisieren/löschen
GET    /{id}                  User-Profil (eingeschränkt)
GET    /                      User-Liste (Admin)
GET    /search?q={query}      User-Suche (für Messaging User-Picker)
PUT    /{id}/roles            Rollen ändern (Admin)
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
POST   /                       Raum erstellen
GET    /{id}                   Raum-Details
PUT    /{id}                   Raum bearbeiten
PUT    /{id}/settings          Raum-Einstellungen
POST   /{id}/members           Mitglied hinzufügen
DELETE /{id}/members/{userId}  Mitglied entfernen
GET    /{id}/members           Mitgliederliste
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
GET    /                       Offene Jobs
POST   /                       Job erstellen
GET    /{id}                   Job-Detail
PUT    /{id}                   Job bearbeiten
POST   /{id}/apply             Für Job anmelden
PUT    /{id}/assignments/{aid} Zuweisung aktualisieren
POST   /{id}/assignments/{aid}/confirm  Stunden bestätigen
GET    /report                 Admin: Stunden-Übersicht
GET    /report/export          Admin: CSV-Export
GET    /report/pdf             Admin: PDF-Export
```

### Cleaning (`/api/v1/cleaning`) [Modul: cleaning]
```
GET    /configs                Putz-Konfigurationen (Admin)
POST   /configs                Konfiguration anlegen
POST   /configs/{id}/generate  Termine generieren
GET    /configs/{id}/qr-codes  QR-Codes als PDF (Admin)
GET    /slots                  Offene Putztermine
GET    /slots/mine             Meine Termine
POST   /slots/{id}/register   Anmelden (Opt-in)
DELETE /slots/{id}/register    Abmelden
POST   /slots/{id}/checkin     QR-Check-in
POST   /slots/{id}/checkout    Check-out
GET    /dashboard              Admin-Dashboard
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

### Notifications (`/api/v1/notifications`)
```
GET    /                       Meine Benachrichtigungen
PUT    /{id}/read              Als gelesen markieren
PUT    /read-all               Alle als gelesen
WS     /ws/notifications       WebSocket Push
GET    /push/public-key        VAPID Public Key
POST   /push/subscribe         Push-Subscription registrieren
POST   /push/unsubscribe       Push-Subscription entfernen
```

### Admin (`/api/v1/admin`)
```
GET    /config                 Systemkonfiguration
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

**Voraussetzung:** Docker + Node.js. Java ist NICHT lokal nötig (Backend wird via Docker kompiliert).

```bash
# 1. Infrastruktur starten (Postgres:5433, Redis:6380, MinIO:9000)
docker compose -f docker-compose.dev.yml up -d

# 2. Backend kompilieren und starten (via Docker)
docker run --rm -v "E:/claude/montessori/backend:/app" -w //app maven:3.9-eclipse-temurin-21 mvn clean package -DskipTests
docker compose up backend

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
# Frontend (87 Tests, ~1.5s)
cd frontend && npm test

# Backend (Testcontainers, Docker required)
cd backend && mvn test
```

---

## Abgeschlossene Erweiterungen

- [x] E-Mail-Versand (SMTP, conditional via `monteweb.email.enabled`)
- [x] Englische Übersetzung (`de.ts` + `en.ts`, LanguageSwitcher, Browser-Locale-Detection)
- [x] Testabdeckung (87 Frontend-Tests via Vitest, Backend Integration Tests via Testcontainers)
- [x] CI/CD Pipeline (`.github/workflows/ci.yml` — Backend, Frontend, Docker Jobs)
- [x] OIDC/SSO (OAuth2 Client, conditional via `monteweb.oidc.enabled`, SSO-Button in Login)
- [x] PDF-Export (Stundenbericht + QR-Codes via OpenHTMLToPDF)
- [x] Push Notifications (VAPID Web Push, conditional via `monteweb.push.enabled`)
- [x] Monitoring (Prometheus + Grafana, Docker Compose Profile `monitoring`)
- [x] Messaging User-Picker (NewMessageDialog mit AutoComplete-Suche)
- [x] Kommunikationsregeln (Eltern-Eltern, Schüler-Schüler konfigurierbar)
- [x] Raum-Diskussions-Threads (LEADER erstellt/archiviert/löscht, Mitglieder antworten)
- [x] Kalender/Events (Raum/Bereich/Schulweit, RSVP, Monatsnavigation, conditional via `monteweb.modules.calendar.enabled`)

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
| Calendar-Modul | `monteweb.modules.calendar.enabled` | `false` |
