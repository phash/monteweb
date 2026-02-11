# MonteWeb

Modulares, selbst-gehostetes Schul-Intranet fuer Montessori-Schulkomplexe (Krippe bis Oberstufe).

## Screenshots

| Login | Dashboard | Kalender |
|-------|-----------|----------|
| ![Login](screenshots/01-login.png) | ![Dashboard](screenshots/02-dashboard.png) | ![Kalender](screenshots/09-calendar.png) |

## Features

- **Feed & Nachrichten** — Schulweiter Newsfeed mit Posts, Kommentaren und System-Bannern
- **Raeume** — Klassen, Gruppen und Projekte mit Mitgliederverwaltung, Diskussions-Threads und Chat
- **Familienverbund** — Familien mit Einladungscodes, Stundenkonto und Kinderzuordnung
- **Jobboerse** — Elternstunden-Verwaltung mit Anmeldung, Bestaetigungen und PDF-Export
- **Putz-Organisation** — Putztermine mit QR-Check-in/-out und PDF-Export der QR-Codes
- **Kalender** — Events auf Raum-, Bereichs- oder Schulebene mit RSVP
- **Direktnachrichten** — Echtzeit-Chat mit WebSocket und konfigurierbaren Kommunikationsregeln
- **Dateiverwaltung** — Dateiablage pro Raum via MinIO (S3-kompatibel)
- **Benachrichtigungen** — In-App + optionale Web Push Notifications (VAPID)
- **OIDC/SSO** — Optionale Anbindung an externe Identity Provider
- **i18n** — Deutsch + Englisch mit Browser-Locale-Erkennung
- **DSGVO** — Datenexport und Account-Loeschung
- **Admin** — Benutzerverwaltung, Modulsteuerung, Theme-Anpassung, Audit-Log
- **Monitoring** — Prometheus + Grafana Dashboard (optional)

## Tech-Stack

### Backend
Java 21, Spring Boot 3.4, Spring Modulith, Spring Security (JWT), Spring Data JPA + Flyway, Spring WebSocket + Redis Pub/Sub, PostgreSQL 16, Redis 7, MinIO

### Frontend
Vue 3.5 (Composition API), TypeScript 5.9, Vite 7, PrimeVue 4 (Aura), Pinia 3, vue-i18n, Axios, PWA

### Infrastruktur
Docker Compose, nginx (Reverse Proxy), GitHub Actions CI/CD, Prometheus + Grafana

## Schnellstart

### Voraussetzungen

- Docker + Docker Compose
- Node.js (fuer Frontend-Entwicklung)

### Production

```bash
cp .env.example .env
# .env anpassen (Datenbank-Passwoerter, JWT-Secret, etc.)
docker compose up -d
```

Die Anwendung ist erreichbar unter `http://localhost:8091`.

### Entwicklung

```bash
# 1. Infrastruktur starten (Postgres, Redis, MinIO)
docker compose -f docker-compose.dev.yml up -d

# 2. Backend starten (via Docker)
docker compose up backend

# 3. Frontend starten
cd frontend
npm install
npm run dev
```

## Tests

```bash
# Frontend (87 Tests, ~1.5s)
cd frontend && npm test

# Backend (Testcontainers, Docker erforderlich)
cd backend && mvn test
```

## Port-Belegung (Entwicklung)

| Service | Port |
|---------|------|
| PostgreSQL | 5433 |
| Redis | 6380 |
| MinIO API / Console | 9000 / 9001 |
| Backend | 8090 |
| Frontend (dev) | 5173 |
| Frontend (Docker) | 8091 |

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
│   │   ├── feed/          # Newsfeed, Posts, Kommentare
│   │   ├── calendar/      # Kalender, Events, RSVP
│   │   ├── notification/  # Benachrichtigungen, Web Push
│   │   ├── messaging/     # Direktnachrichten, Chat
│   │   ├── files/         # Dateiablage (MinIO)
│   │   ├── jobboard/      # Jobboerse, Elternstunden
│   │   ├── cleaning/      # Putz-Organisation, QR-Codes
│   │   ├── admin/         # Systemkonfiguration, Audit-Log
│   │   └── shared/        # Security, DTOs, Exceptions
│   └── Dockerfile
├── frontend/
│   ├── src/
│   │   ├── views/         # Seiten (Login, Dashboard, Rooms, ...)
│   │   ├── components/    # UI-Komponenten
│   │   ├── stores/        # Pinia State Management
│   │   ├── api/           # Axios API-Client
│   │   ├── types/         # TypeScript Interfaces
│   │   ├── composables/   # Composables (Auth, WebSocket, ...)
│   │   └── i18n/          # Deutsch + Englisch
│   └── Dockerfile
├── monitoring/            # Prometheus + Grafana Config
├── nginx/                 # Production Reverse Proxy
├── docker-compose.yml     # Production Stack
└── docker-compose.dev.yml # Entwicklungs-Infrastruktur
```

## Optionale Module

Module koennen ueber Konfiguration aktiviert/deaktiviert werden:

| Modul | Config-Property | Standard |
|-------|----------------|----------|
| Messaging | `monteweb.modules.messaging.enabled` | `true` |
| Dateiverwaltung | `monteweb.modules.files.enabled` | `true` |
| Jobboerse | `monteweb.modules.jobboard.enabled` | `true` |
| Putz-Organisation | `monteweb.modules.cleaning.enabled` | `true` |
| Kalender | `monteweb.modules.calendar.enabled` | `false` |
| E-Mail-Versand | `monteweb.email.enabled` | `false` |
| OIDC/SSO | `monteweb.oidc.enabled` | `false` |
| Push Notifications | `monteweb.push.enabled` | `false` |

## Monitoring (optional)

```bash
docker compose --profile monitoring up -d
# Grafana: http://localhost:3000 (admin/admin)
# Prometheus: http://localhost:9090
```

## Lizenz

Dieses Projekt ist nicht unter einer Open-Source-Lizenz veroeffentlicht. Alle Rechte vorbehalten.
