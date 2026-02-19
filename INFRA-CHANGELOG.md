# Infrastruktur-Changelog — MonteWeb

> Erstellt: 2026-02-19

---

## Durchgeführte Optimierungen

### 1. `.dockerignore`-Dateien erstellt

**Dateien:** `backend/.dockerignore`, `frontend/.dockerignore`

**Begründung:** Ohne `.dockerignore` wurde der gesamte Projektinhalt (inkl. `node_modules/`, `.git/`, `target/`, Test-Dateien, IDE-Configs) in den Docker-Build-Kontext kopiert. Das machte jeden Build unnötig langsam.

**Auswirkung:**
- Frontend Build-Kontext: ~500MB → ~8KB (gemessen: `transferring context: 8.45kB`)
- Backend Build-Kontext: ~200MB+ → wenige KB
- Build-Geschwindigkeit signifikant verbessert

---

### 2. Backend Dockerfile optimiert

**Datei:** `backend/Dockerfile`

**Änderungen:**
- `--no-transfer-progress` bei `mvn` → saubere Build-Logs ohne Download-Balken
- `start_period` im HEALTHCHECK auf 60s erhöht (konsistent mit docker-compose.yml)
- OCI-Labels hinzugefügt (`org.opencontainers.image.*`)

**Auswirkung:** Sauberere Logs, bessere Image-Metadaten, konsistente Health-Check-Timings

---

### 3. Frontend Dockerfile gehärtet

**Datei:** `frontend/Dockerfile`

**Änderungen:**
- nginx läuft jetzt als Non-Root-User (`nginx`) statt Root
- `start_period` zum HEALTHCHECK hinzugefügt (10s)
- OCI-Labels hinzugefügt
- `chown`-Befehle für nginx-Verzeichnisse und PID-File

**Auswirkung:** Sicherheit — Container läuft nicht mehr als Root

---

### 4. nginx Security-Header und Härtung

**Datei:** `frontend/nginx.conf`

**Änderungen:**
- `server_tokens off` — nginx-Version wird nicht mehr offengelegt
- `X-Frame-Options: SAMEORIGIN` — Schutz gegen Clickjacking
- `X-Content-Type-Options: nosniff` — Schutz gegen MIME-Sniffing
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Permissions-Policy: camera=(), microphone=(), geolocation=()` — deaktiviert nicht benötigte Browser-APIs
- `client_max_body_size 50m` — erlaubt Uploads bis 50MB (Fotobox, Chat-Bilder, Fundgrube)
- WebSocket-Timeout auf 3600s erhöht (1 Stunde statt 60s)
- Actuator-Endpunkte: nur `/actuator/health` öffentlich; alle anderen (`/actuator/metrics`, `/actuator/prometheus`) blockiert mit `deny all`

**Auswirkung:** Deutlich verbesserte Sicherheit; Upload-Fehler bei größeren Dateien behoben

---

### 5. Docker Compose: Netzwerk-Isolation und Ressourcen-Limits

**Datei:** `docker-compose.yml`

**Änderungen:**
- **Netzwerk-Isolation:** Zwei getrennte Netzwerke (`monteweb-frontend`, `monteweb-backend`)
  - Frontend (nginx) → Backend: über `frontend`-Netzwerk
  - Backend → Postgres/Redis/MinIO: über `backend`-Netzwerk
  - Frontend hat keinen direkten Zugriff auf Datenbank/Redis/MinIO
- **Ressourcen-Limits:** `deploy.resources.limits.memory` auf allen Services
  - Postgres: 512MB
  - Redis: 256MB
  - MinIO: 512MB
  - Backend: 1GB
  - Frontend (nginx): 128MB
- **MinIO-Version gepinnt:** `minio/minio:RELEASE.2025-01-20T14-49-07Z` statt `latest`

**Auswirkung:** Bessere Sicherheit durch Netzwerk-Segmentierung; Schutz vor Speicher-Überläufen; reproduzierbare MinIO-Version

---

### 6. CI/CD-Pipeline optimiert

**Datei:** `.github/workflows/ci.yml`

**Änderungen:**
- **SHA-Pinning:** Alle Actions auf Commit-SHA gepinnt (mit Versions-Kommentar)
  - `actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683` (v4.2.2)
  - `actions/setup-java@c1e323688fd81a25caa38c78aa6df2d33d3e20d9` (v4)
  - `actions/setup-node@49933ea5288caeca8642d1e84afbd3f7d6820020` (v4)
  - `docker/setup-buildx-action` und `docker/build-push-action` (SHA-gepinnt)
  - `aquasecurity/trivy-action` (SHA-gepinnt)
- **Concurrency-Group:** `ci-${{ github.ref }}` mit `cancel-in-progress: true` — doppelte Runs werden abgebrochen
- **Job-Timeouts:** Backend 15min, Frontend 10min, Docker 20min
- **Docker-Layer-Caching:** `docker/build-push-action` mit GitHub Actions Cache (`cache-from/to: type=gha`)
- **Container-Image-Scanning:** Trivy scannt beide Images auf CRITICAL/HIGH Schwachstellen (exit-code: 0 = Report only, blockiert Build nicht)
- **`--no-transfer-progress`** bei Maven-Befehlen
- **Docker Buildx:** Eingerichtet für effizienteres Build-Caching

**Auswirkung:** Supply-Chain-Sicherheit; schnellere Builds durch Caching; keine verschwendeten CI-Minuten durch doppelte Runs; Schwachstellen-Erkennung

---

### 7. Dependabot konfiguriert

**Datei:** `.github/dependabot.yml`

**Konfiguriert für:**
- GitHub Actions (wöchentlich)
- npm-Dependencies Frontend (wöchentlich, gruppiert nach dev/prod)
- Maven-Dependencies Backend (wöchentlich)
- Docker Base-Images (wöchentlich, je Dockerfile)

**Auswirkung:** Automatische PRs für veraltete Dependencies; Sicherheitslücken werden zeitnah erkannt

---

## Zusammenfassung der Auswirkungen

| Bereich | Vorher | Nachher |
|---|---|---|
| Frontend Build-Kontext | ~500MB | ~8KB |
| nginx User | Root | Non-Root (`nginx`) |
| Security-Header | Keine | 4 Header + server_tokens off |
| Upload-Limit | 1MB (Standard) | 50MB |
| Actuator öffentlich | Alle Endpunkte | Nur `/health` |
| Netzwerk-Isolation | Kein (Default) | 2 getrennte Netzwerke |
| Ressourcen-Limits | Keine | Auf allen Services |
| MinIO-Version | `latest` | Gepinnt |
| Actions SHA-Pinning | Nein | Ja (alle) |
| CI Concurrency | Keine | cancel-in-progress |
| CI Job-Timeout | 6h (Standard) | 10-20 min |
| Docker-Caching in CI | Kein | GitHub Actions Cache |
| Image-Scanning | Kein | Trivy (CRITICAL+HIGH) |
| Dependency-Updates | Manuell | Dependabot (wöchentlich) |
