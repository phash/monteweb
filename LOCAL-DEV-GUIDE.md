# Lokaler Entwicklungs-Leitfaden — MonteWeb

> Verbindliche Referenz für lokale Entwicklung, Tests und Deployment.

---

## 1. Voraussetzungen

### Benötigte Tools

| Tool | Version | Zweck |
|---|---|---|
| Docker Desktop | 4.x+ | Container-Runtime (Backend, Infra, Full-Stack) |
| Docker Compose | v2.x (in Docker Desktop enthalten) | Service-Orchestrierung |
| Node.js | 22.x | Frontend-Entwicklung, Tests |
| npm | 10.x+ (mit Node 22) | Package-Manager |
| Git | 2.x+ | Versionsverwaltung |

**Optional:**
| Tool | Zweck |
|---|---|
| Java 21 (Temurin) | Lokale Backend-Entwicklung ohne Docker |
| Maven 3.9+ | Backend-Builds ohne Docker |
| IDE: IntelliJ IDEA / VS Code | Empfohlene Entwicklungsumgebungen |

> **Hinweis:** Java und Maven sind für reine Frontend-Entwicklung NICHT nötig — das Backend wird via Docker gebaut und gestartet.

### Betriebssystem-Hinweise

- **Windows:** Docker Desktop mit WSL2-Backend empfohlen. Git Bash oder WSL2-Terminal verwenden
- **macOS:** Docker Desktop für Mac. Native Terminal
- **Linux:** Docker Engine + Docker Compose Plugin

---

## 2. Projekt-Setup

### Erstmalige Einrichtung

```bash
# 1. Repository klonen
git clone <repository-url> montessori
cd montessori

# 2. .env-Datei erstellen (aus Vorlage)
cp .env.example .env

# 3. Secrets in .env anpassen:
#    - POSTGRES_PASSWORD: Sicheres Passwort wählen
#    - REDIS_PASSWORD: Sicheres Passwort wählen
#    - JWT_SECRET: Mindestens 64 Zeichen (openssl rand -base64 64)
#    - MINIO_ACCESS_KEY / MINIO_SECRET_KEY: Eigene Zugangsdaten

# 4. Full Stack starten
docker compose up -d

# 5. Prüfen ob alle Services laufen
docker compose ps
```

### Environment-Variablen (.env)

| Variable | Pflicht | Beschreibung | Beispiel |
|---|---|---|---|
| `POSTGRES_PASSWORD` | Ja | PostgreSQL-Passwort | `mySecretPw123` |
| `REDIS_PASSWORD` | Ja | Redis-Passwort | `redisSecret` |
| `JWT_SECRET` | Ja | JWT-Signierungsschlüssel (min. 64 Zeichen) | `openssl rand -base64 64` |
| `MINIO_ACCESS_KEY` | Ja | MinIO-Zugangsdaten | `minioadmin` |
| `MINIO_SECRET_KEY` | Ja | MinIO-Geheimschlüssel | `minioadmin` |
| `MINIO_BUCKET` | Nein | MinIO-Bucket-Name (Standard: `monteweb`) | `monteweb` |
| `APP_PORT` | Nein | Host-Port für die App (Standard: `80`) | `80` |
| `FRONTEND_URL` | Nein | URL der App (Standard: `http://localhost`) | `http://localhost` |
| `EMAIL_ENABLED` | Nein | E-Mail-Versand aktivieren (Standard: `false`) | `false` |
| `OIDC_ENABLED` | Nein | SSO aktivieren (Standard: `false`) | `false` |
| `PUSH_ENABLED` | Nein | Push-Benachrichtigungen (Standard: `false`) | `false` |

### Datenbank & Seed-Daten

Die Datenbank wird beim ersten Start automatisch initialisiert:
- **Flyway-Migrationen** (V001–V061) laufen automatisch
- **Seed-Daten** (V040) erstellen ~220 Test-Benutzer, Räume, Schulbereiche

**Test-Accounts** (Passwort: `test1234`):

| E-Mail | Rolle | Räume |
|---|---|---|
| `admin@monteweb.local` | SUPERADMIN | Alle 8 Räume |
| `lehrer@monteweb.local` | TEACHER | Sonnengruppe |
| `eltern@monteweb.local` | PARENT | Sonnengruppe |
| `schueler@monteweb.local` | STUDENT | Sonnengruppe |
| `sectionadmin@monteweb.local` | SECTION_ADMIN | Sonnengruppe + Sternengruppe |

---

## 3. Lokale Docker-Nutzung

### Service-Architektur

```
┌───────────────────────────────────────────────────────────────┐
│                    Docker Compose Stack                        │
│                                                               │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌──────────┐  │
│  │ Frontend │───▶│ Backend │───▶│Postgres │    │  MinIO   │  │
│  │ (nginx)  │    │ (Spring)│───▶│  (DB)   │    │ (Dateien)│  │
│  │ :80      │    │ :8080   │───▶│  :5432  │    │ :9000    │  │
│  └─────────┘    └─────────┘    └─────────┘    └──────────┘  │
│       │              │              │                         │
│  frontend-net   frontend-net  backend-net     backend-net    │
│  + backend-net                                               │
│                      │         ┌─────────┐                   │
│                      └────────▶│  Redis   │                   │
│                                │ (Cache)  │                   │
│                                │ :6379    │                   │
│                                └─────────┘                   │
└───────────────────────────────────────────────────────────────┘
```

**Netzwerke:**
- `monteweb-frontend`: nginx ↔ Backend (API-Proxy)
- `monteweb-backend`: Backend ↔ Postgres, Redis, MinIO

### Befehle

```bash
# ── Starten ──────────────────────────────────────────────────
docker compose up -d                          # Alle Services starten
docker compose --profile monitoring up -d     # Mit Grafana + Prometheus

# ── Stoppen ──────────────────────────────────────────────────
docker compose down                           # Services stoppen
docker compose down -v                        # + Volumes löschen (⚠️ DB-Daten weg!)

# ── Neustarten ───────────────────────────────────────────────
docker compose restart backend                # Einzelnen Service neustarten
docker compose up -d --force-recreate backend # Container neu erstellen

# ── Bauen ────────────────────────────────────────────────────
docker compose build                          # Alle Images bauen
docker compose build backend                  # Nur Backend
docker compose build frontend                 # Nur Frontend
docker compose build --no-cache backend       # Ohne Layer-Cache (bei Problemen)

# ── Bauen + Starten (häufigster Workflow) ────────────────────
docker compose build backend && docker compose up backend -d
docker compose build frontend && docker compose up frontend -d
docker compose build && docker compose up -d  # Alles

# ── Status ───────────────────────────────────────────────────
docker compose ps                             # Service-Status
docker compose logs backend                   # Backend-Logs
docker compose logs -f backend                # Live-Logs (follow)
docker compose logs --tail=100 backend        # Letzte 100 Zeilen

# ── Debugging ────────────────────────────────────────────────
docker compose exec backend sh                # Shell in Backend-Container
docker compose exec postgres psql -U monteweb # Direkt in PostgreSQL
docker compose exec redis redis-cli           # Redis-CLI
```

### Dev-Modus: Nur Infrastruktur

Für lokale Backend-/Frontend-Entwicklung mit Hot-Reload:

```bash
# 1. Infrastruktur starten (Postgres:5433, Redis:6380, MinIO:9000)
docker compose -f docker-compose.dev.yml up -d

# 2. Backend lokal starten (benötigt Java 21 + Maven)
cd backend && ./mvnw spring-boot:run

# 3. Frontend mit Hot-Reload (separates Terminal)
cd frontend && npm install && npm run dev     # http://localhost:5173
```

**Achtung:** Im Dev-Modus nutzt der Frontend-Dev-Server (Vite) den Proxy aus `vite.config.ts`, der API-Calls an `localhost:8080` weiterleitet.

### Unterschiede Dev vs. Produktion

| Aspekt | Dev (docker-compose.dev.yml) | Produktion (docker-compose.yml) |
|---|---|---|
| Infrastruktur-Ports | Exponiert (5433, 6380, 9000) | Nicht exponiert (intern) |
| Backend | Lokal oder Docker | Docker |
| Frontend | Vite Dev Server (:5173) | nginx (:80) |
| Hot-Reload | Ja (Vite HMR) | Nein (Rebuild nötig) |
| Passwörter | Default (`changeme`) | Aus `.env` (pflicht) |
| Restart-Policy | Keine | `unless-stopped` |
| Netzwerk-Isolation | Nein | Ja (2 Netzwerke) |
| Ressourcen-Limits | Keine | Ja (Memory-Limits) |

---

## 4. Lokale Test-Ausführung

### Frontend-Tests (Vitest)

```bash
cd frontend

# Alle Tests ausführen
npm test                                      # = npx vitest run

# Tests im Watch-Modus (TDD)
npm run test:watch

# Tests mit Coverage-Report
npm run test:coverage

# TypeScript-Check (ohne Ausführung)
npx vue-tsc --noEmit

# JSON-Reporter (für maschinelles Auslesen)
npx vitest run --reporter=json 2>/dev/null | python3 -c "
import sys,json
d=json.load(sys.stdin)
print(f'Tests: {d[\"numTotalTests\"]} passed={d[\"numPassedTests\"]} failed={d[\"numFailedTests\"]}')
print(f'Success: {d[\"success\"]}')
"

# Einzelne Test-Datei
npx vitest run src/views/__tests__/LoginView.test.ts

# Tests mit Pattern-Filter
npx vitest run --testPathPattern="admin"
```

**Aktuelle Metriken:** ~1077 Tests in ~118 Dateien, ~57% Statement-Coverage

### Backend-Tests (JUnit + Testcontainers)

```bash
cd backend

# Alle Tests (benötigt Docker für Testcontainers!)
./mvnw test

# Einzelne Testklasse
./mvnw test -Dtest=AuthControllerIntegrationTest

# Einzelne Testmethode
./mvnw test -Dtest="AuthControllerIntegrationTest#register_*"

# Tests überspringen (nur Kompilierung)
./mvnw compile -DskipTests
```

**Hinweis:** Testcontainers startet automatisch Postgres- und Redis-Container. Docker muss laufen!

### Build-Verifizierung (Docker)

```bash
# Backend-Build prüfen (Maven inside Docker)
docker compose build backend 2>&1 > /tmp/build_output.txt
grep "BUILD SUCCESS" /tmp/build_output.txt && echo "OK" || echo "FAILED"
grep "\[ERROR\]" /tmp/build_output.txt       # Java-Kompilierungsfehler finden

# Frontend-Build prüfen
docker compose build frontend 2>&1 > /tmp/build_frontend.txt
grep "error\|ERROR" /tmp/build_frontend.txt   # Fehler suchen
```

**Hinweis:** `docker compose build` gibt auf manchen Systemen (Windows) Exit-Code 1 zurück, obwohl der Build erfolgreich war. Immer `BUILD SUCCESS` oder `error` im Output prüfen!

### Vollständige Validierung vor PR

```bash
# 1. Frontend: TypeScript + Tests
cd frontend && npx vue-tsc --noEmit && npm test

# 2. Backend: Docker-Build (inkl. Maven-Compile)
cd .. && docker compose build --no-cache backend

# 3. Full-Stack-Smoke-Test
docker compose up -d
docker compose ps    # Alle Services "healthy"?
# http://localhost → App erreichbar?
```

---

## 5. Deployment-Workflow

### Lokaler Build = CI/CD-Build

Die CI-Pipeline (`.github/workflows/ci.yml`) führt dieselben Schritte aus wie der lokale Build:

| CI-Job | Lokales Äquivalent |
|---|---|
| `backend` → Compile + Test | `cd backend && ./mvnw test` |
| `frontend` → Build + Test | `cd frontend && npm run build && npm run test:coverage` |
| `docker` → Image Build | `docker compose build` |

### Images lokal bauen und taggen

```bash
# Images mit Tag bauen
docker build -t monteweb-backend:v1.0.0 ./backend
docker build -t monteweb-frontend:v1.0.0 ./frontend

# Image-Größe prüfen
docker images monteweb-*
```

### Deployment-Checkliste

1. Alle Frontend-Tests grün (`npm test`)
2. TypeScript kompiliert (`vue-tsc --noEmit`)
3. Backend baut erfolgreich (`docker compose build backend` → `BUILD SUCCESS`)
4. Full-Stack startet (`docker compose up -d` → alle Container `healthy`)
5. Manuelle Prüfung: App unter http://localhost erreichbar
6. Commit + Push → CI-Pipeline grün

---

## 6. Troubleshooting

### Häufige Probleme

#### Container startet nicht / bleibt in "starting"

```bash
# Logs prüfen
docker compose logs backend

# Health-Status prüfen
docker inspect --format='{{json .State.Health}}' monteweb-backend | python3 -m json.tool
```

**Häufige Ursachen:**
- `.env`-Datei fehlt oder Pflichtfelder leer → `POSTGRES_PASSWORD is required`
- Port bereits belegt → `bind: address already in use`
- Backend wartet auf DB → `depends_on: service_healthy` sollte das lösen; ggf. Postgres-Logs prüfen

#### Port-Konflikte

```bash
# Welcher Prozess belegt Port 80?
# Windows: netstat -ano | findstr :80
# Linux/Mac: lsof -i :80

# App-Port in .env ändern:
APP_PORT=8080
```

#### Docker-Build nimmt alte Sourcen

```bash
# Layer-Cache komplett umgehen
docker compose build --no-cache backend

# Oder nur den COPY-Layer invalidieren
docker compose build --build-arg CACHEBUST=$(date +%s) backend
```

#### Datenbank zurücksetzen

```bash
# ⚠️ Löscht alle Daten!
docker compose down -v
docker compose up -d
# Flyway-Migrationen + Seed-Daten laufen automatisch neu
```

#### "Exit Code 137" (OOM)

Container wurde wegen Speichermangel beendet. In `docker-compose.yml` das Memory-Limit erhöhen:
```yaml
deploy:
  resources:
    limits:
      memory: 2G  # von 1G erhöhen
```

#### MinIO-Bucket existiert nicht

Das Backend erstellt den Bucket automatisch beim Start. Falls manuell nötig:
```bash
docker compose exec minio mc alias set local http://localhost:9000 $MINIO_ACCESS_KEY $MINIO_SECRET_KEY
docker compose exec minio mc mb local/monteweb
```

### Container-Health prüfen

```bash
# Übersicht aller Container mit Health-Status
docker compose ps

# Einzelnen Container inspizieren
docker inspect monteweb-backend --format='{{.State.Health.Status}}'

# Health-Check-Logs
docker inspect monteweb-backend --format='{{range .State.Health.Log}}{{.Output}}{{end}}'
```

### Aufräumen

```bash
# Gestoppte Container entfernen
docker compose down

# Ungenutzte Images entfernen
docker image prune -f

# Alles aufräumen (⚠️ entfernt auch Volumes anderer Projekte!)
docker system prune -a --volumes
```

---

## 7. Konventionen und Regeln

### Docker-Naming

| Element | Konvention | Beispiel |
|---|---|---|
| Container | `monteweb-<service>` | `monteweb-backend` |
| Image (lokal) | `montessori-<service>` | `montessori-backend` |
| Netzwerk | `monteweb-<name>` | `monteweb-frontend` |
| Volume | `montessori_<name>` | `montessori_postgres_data` |

### Branch-Strategie

- **`main`:** Stabiler Branch. CI läuft bei Push und PRs
- **Feature-Branches:** `feature/beschreibung` oder `issue-XX-beschreibung`
- Docker-Build-Job in CI nur auf `main` (nicht bei PRs)

### Neuen Service hinzufügen

1. Dockerfile im Service-Verzeichnis erstellen
2. `.dockerignore` für den Service erstellen
3. Service in `docker-compose.yml` eintragen (mit Health-Check, Netzwerk, Resource-Limits)
4. Falls Dev-relevant: auch in `docker-compose.dev.yml` eintragen
5. `.env.example` um neue Variablen ergänzen
6. CI-Workflow anpassen falls nötig
7. Diesen Guide aktualisieren

### PR-Checkliste

- [ ] Frontend: `npx vue-tsc --noEmit` erfolgreich
- [ ] Frontend: `npm test` — alle Tests grün
- [ ] Backend: `docker compose build backend` → `BUILD SUCCESS`
- [ ] Full-Stack: `docker compose up -d` → alle Services healthy
- [ ] App unter http://localhost erreichbar und funktional
- [ ] Neue Env-Variablen in `.env.example` dokumentiert
- [ ] Neue Flyway-Migrationen: nächste freie Versionsnummer (`V0XX__beschreibung.sql`)
- [ ] Keine Secrets im Code oder in Images

---

## Monitoring (Optional)

```bash
# Monitoring-Stack starten
docker compose --profile monitoring up -d

# Grafana: http://localhost:3000 (admin/admin)
# Prometheus: http://localhost:9090
```

Vorkonfiguriertes Dashboard: "MonteWeb Overview" mit JVM-Metriken, HTTP-Request-Raten, Datenbankverbindungen.
