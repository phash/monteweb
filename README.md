# MonteWeb

Modulares, selbst-gehostetes Schul-Intranet fuer Montessori-Schulkomplexe (Krippe bis Oberstufe).

## Screenshots

| Login | Dashboard | Kalender |
|-------|-----------|----------|
| ![Login](screenshots/01-login.png) | ![Dashboard](screenshots/02-dashboard.png) | ![Kalender](screenshots/09-calendar.png) |

## Features

- **Feed & Nachrichten** — Schulweiter Newsfeed mit Posts, Kommentaren, System-Bannern und gezielten Posts fuer bestimmte Nutzer
- **Raeume** — Klassen, Gruppen und Projekte mit Mitgliederverwaltung, Diskussions-Threads, Chat und Beitrittsanfragen
- **Familienverbund** — Familien mit Einladungscodes, Stundenkonto, Kinderzuordnung und optionaler Stundenbefreiung
- **Jobboerse** — Elternstunden-Verwaltung mit Anmeldung, Bestaetigungen, Jahresabrechnung und PDF-Export
- **Putz-Organisation** — Putzaktionen mit automatischer Kalender-Event- und Job-Erstellung, Familien-Anmeldeliste
- **Kalender** — Events auf Raum-, Bereichs- oder Schulebene mit RSVP, Absage-Benachrichtigungen und iCal-Export
- **Formulare** — Umfragen und Einverstaendniserklaerungen mit Multi-Bereichs-Targeting, Dashboard-Widget, CSV/PDF-Export
- **Fotobox** — Foto-Threads pro Raum mit Thumbnails, Lightbox und Zielgruppen-Sichtbarkeit
- **Fundgrube** — Schulweite Fundgrube: Verlorene Gegenstaende mit Foto melden, nach Schulbereich filtern, Besitzer koennen beanspruchen (automatische Entfernung nach 24h)
- **Direktnachrichten** — Echtzeit-Chat mit WebSocket und konfigurierbaren Kommunikationsregeln
- **Dateiverwaltung** — Dateiablage pro Raum via MinIO (S3-kompatibel) mit Ordner-Zielgruppen und Auto-Ordner fuer Klassen
- **Benachrichtigungen** — In-App + optionale Web Push Notifications (VAPID)
- **PWA** — Installierbar auf Smartphones, Offline-Zwischenspeicherung eigener Inhalte (Termine, Jobs)
- **Bereichsverwaltung** — Section-Admin-Rolle mit eigenem Verwaltungspanel
- **OIDC/SSO** — Optionale Anbindung an externe Identity Provider
- **i18n** — Deutsch + Englisch mit Browser-Locale-Erkennung
- **Feiertage & Ferien** — Bundesland-abhaengige Feiertage (alle 16 Bundeslaender) und konfigurierbare Schulferien
- **DSGVO** — Datenexport und Account-Loeschung
- **Fehlerberichterstattung** — Automatisches Error-Reporting mit GitHub-Issue-Integration
- **Lesezeichen** — Posts, Events, Jobs und Wiki-Seiten als Lesezeichen speichern
- **Aufgaben (Kanban)** — Kanban-Board pro Raum: Aufgaben erstellen, zuweisen und in Spalten verwalten
- **Wiki** — Wiki pro Raum: Markdown-Seiten mit Hierarchie, Versionierung und Suchfunktion
- **Benutzerdefinierte Profilfelder** — Administratoren koennen zusaetzliche Felder fuer Benutzerprofile anlegen
- **Volltextsuche** — Globale Suche (Ctrl+K) mit Solr-Volltextsuche, Tika-Extraktion fuer Dokumente
- **2FA** — Optionale/Verpflichtende Zwei-Faktor-Authentifizierung (TOTP)
- **LDAP/AD** — Authentifizierung ueber externen Verzeichnisdienst
- **Jitsi** — Video-Meetings in Kalender-Terminen und Raum-Chats
- **ONLYOFFICE** — Dokumente direkt im Browser bearbeiten (Word, Excel, PowerPoint)
- **ClamAV** — Automatische Virenscanner fuer Datei-Uploads
- **Admin** — Benutzerverwaltung, Modulsteuerung, Theme-Anpassung, Audit-Log, Fehlerberichte, CSV-Import
- **Monitoring** — Prometheus + Grafana Dashboard (optional)

## Tech-Stack

### Backend
Java 21, Spring Boot 3.4, Spring Modulith 1.3, Spring Security (JWT), Spring Data JPA + Flyway (96 Migrationen), Spring WebSocket + Redis Pub/Sub, PostgreSQL 16, Redis 7, MinIO, Apache Solr 9.8

### Frontend
Vue 3.5 (Composition API), TypeScript 5.9, Vite 7, PrimeVue 4 (Aura), Pinia 3, vue-i18n, Axios, PWA (vite-plugin-pwa + Workbox)

### Infrastruktur
Docker Compose, nginx (Reverse Proxy), Caddy (Auto-SSL), GitHub Actions CI/CD, Prometheus + Grafana

### Tests
- Frontend: ~1341 Tests in 147 Dateien (Vitest + vue-test-utils)
- Backend: 47 Testklassen, ~490 Tests (Testcontainers + MockMvc)

## Schnellstart

### Voraussetzungen

- Docker + Docker Compose
- Node.js 20+ (fuer Frontend-Entwicklung)

### Production

```bash
cp .env.example .env
# .env anpassen (Datenbank-Passwoerter, JWT-Secret, etc.)
docker compose up -d
```

Die Anwendung ist erreichbar unter `http://localhost` (Port 80).

### Entwicklung

```bash
# 1. Infrastruktur starten (Postgres, Redis, MinIO)
docker compose -f docker-compose.dev.yml up -d

# 2. Full Stack via Docker
docker compose up -d

# 3. Oder: Frontend mit Hot Reload (erfordert laufendes Backend)
cd frontend
npm install
npm run dev   # http://localhost:5173
```

## Tests

```bash
# Frontend (~1341 Tests)
cd frontend && npm test

# Backend (Testcontainers, Docker erforderlich)
cd backend && ./mvnw test
```

## Port-Belegung

| Service | Entwicklung | Docker Compose |
|---------|-------------|----------------|
| PostgreSQL | 5433 | 5432 (intern) |
| Redis | 6380 | 6379 (intern) |
| MinIO API / Console | 9000 / 9001 | 9000 / 9001 |
| Backend | 8080 | 8080 (intern) |
| Frontend (dev) | 5173 | — |
| nginx (Production) | — | 80 |

## Projektstruktur

```
monteweb/
├── backend/
│   ├── src/main/java/com/monteweb/
│   │   ├── auth/          # Authentifizierung, JWT, OIDC
│   │   ├── user/          # Benutzerverwaltung, Profile, Rollen
│   │   ├── family/        # Familienverbund, Stundenkonto
│   │   ├── school/        # Schulbereiche (Krippe–Oberstufe)
│   │   ├── room/          # Raeume, Diskussions-Threads
│   │   ├── feed/          # Newsfeed, Posts, Kommentare, Banner
│   │   ├── calendar/      # Kalender, Events, RSVP
│   │   ├── notification/  # Benachrichtigungen, Web Push
│   │   ├── messaging/     # Direktnachrichten, Chat
│   │   ├── files/         # Dateiablage (MinIO), Ordner-Sichtbarkeit
│   │   ├── jobboard/      # Jobboerse, Elternstunden, Jahresabrechnung
│   │   ├── cleaning/      # Putzaktionen, Familien-Anmeldung
│   │   ├── forms/         # Formulare, Umfragen, Export
│   │   ├── fotobox/       # Foto-Threads, Thumbnails
│   │   ├── fundgrube/     # Fundgrube (Lost & Found)
│   │   ├── bookmark/      # Lesezeichen
│   │   ├── tasks/         # Kanban-Board pro Raum
│   │   ├── wiki/          # Wiki pro Raum (Markdown)
│   │   ├── profilefields/ # Benutzerdefinierte Profilfelder
│   │   ├── search/        # Globale Suche (Solr)
│   │   ├── admin/         # Systemkonfiguration, Audit-Log, Error-Reports
│   │   └── shared/        # Security, DTOs, Exceptions, PDF-Service
│   └── Dockerfile
├── frontend/
│   ├── src/
│   │   ├── views/         # Seiten (Login, Dashboard, Rooms, ...)
│   │   ├── components/    # UI-Komponenten nach Domaene
│   │   ├── stores/        # Pinia State Management
│   │   ├── api/           # Axios API-Client
│   │   ├── types/         # TypeScript Interfaces
│   │   ├── composables/   # Composables (Auth, WebSocket, Holidays, ...)
│   │   └── i18n/          # Deutsch + Englisch
│   └── Dockerfile
├── monitoring/            # Prometheus + Grafana Config
├── nginx/                 # Production Reverse Proxy
├── docker-compose.yml     # Production Stack
└── docker-compose.dev.yml # Entwicklungs-Infrastruktur
```

## Module

Module koennen ueber Konfiguration aktiviert/deaktiviert werden:

| Modul | Config-Property | Standard |
|-------|----------------|----------|
| Messaging | `monteweb.modules.messaging.enabled` | `true` |
| Dateiverwaltung | `monteweb.modules.files.enabled` | `true` |
| Jobboerse | `monteweb.modules.jobboard.enabled` | `true` |
| Putz-Organisation | `monteweb.modules.cleaning.enabled` | `true` |
| Kalender | `monteweb.modules.calendar.enabled` | `true` |
| Formulare | `monteweb.modules.forms.enabled` | `true` |
| Fotobox | `monteweb.modules.fotobox.enabled` | `true` |
| Fundgrube | `monteweb.modules.fundgrube.enabled` | `true` |
| Lesezeichen | `monteweb.modules.bookmarks.enabled` | `true` |
| Aufgaben (Kanban) | `monteweb.modules.tasks.enabled` | `true` |
| Wiki | `monteweb.modules.wiki.enabled` | `true` |
| Profilfelder | `monteweb.modules.profilefields.enabled` | `true` |
| Solr-Volltextsuche | `monteweb.modules.solr.enabled` | `false` |
| E-Mail-Versand | `monteweb.email.enabled` | `false` |
| OIDC/SSO | `monteweb.oidc.enabled` | `false` |
| Push Notifications | `monteweb.push.enabled` | `false` |

Zusaetzliche DB-Toggles (Admin UI → Module):

| Toggle | Beschreibung |
|--------|-------------|
| jitsi | Jitsi-Videokonferenzen |
| wopi | ONLYOFFICE-Integration |
| clamav | ClamAV-Virenscanner |
| maintenance | Wartungsmodus |
| ldap | LDAP/Active Directory |
| directoryAdminOnly | Verzeichnis nur fuer Admins |

## Test-Accounts

| Account | Rolle | Passwort |
|---------|-------|----------|
| `admin@monteweb.local` | SUPERADMIN | `admin123` |
| `lehrer@monteweb.local` | TEACHER | `test1234` |
| `eltern@monteweb.local` | PARENT | `test1234` |
| `schueler@monteweb.local` | STUDENT | `test1234` |
| `sectionadmin@monteweb.local` | SECTION_ADMIN | `test1234` |

Plus ~220 realistische Seed-Benutzer (z.B. `anna.mueller@monteweb.local`).

## Monitoring (optional)

```bash
docker compose --profile monitoring up -d
# Grafana: http://localhost:3000 (admin/admin)
# Prometheus: http://localhost:9090
```

## Beitragen

Beitraege sind willkommen! Bitte lies die [CONTRIBUTING.md](CONTRIBUTING.md) fuer Details zu Workflow, Coding-Standards und CLA.

## Lizenz

Dieses Projekt ist lizenziert unter der [Apache License 2.0](LICENSE).
