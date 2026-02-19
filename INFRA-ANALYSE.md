# Infrastruktur-Analyse — MonteWeb

> Erstellt: 2026-02-19 | Projekt: MonteWeb (Schul-Intranet)

---

## 1. Dateien-Inventar

| Datei | Rolle |
|---|---|
| `backend/Dockerfile` | Multi-Stage Build: Maven 3.9 + Temurin 21 → JRE 21 Alpine |
| `frontend/Dockerfile` | Multi-Stage Build: Node 22 Alpine → nginx Alpine |
| `frontend/nginx.conf` | Reverse Proxy (API + WS) + SPA-Fallback + Gzip + Static Cache |
| `docker-compose.yml` | Produktions-Stack: Postgres 16, Redis 7, MinIO, Backend, Frontend + opt. Monitoring |
| `docker-compose.dev.yml` | Dev-Infrastruktur: Postgres (5433), Redis (6380), MinIO (9000/9001) |
| `.env.example` | Dokumentation aller Umgebungsvariablen mit Beispielwerten |
| `.env` | Tatsächliche Secrets (in .gitignore) |
| `.github/workflows/ci.yml` | CI-Pipeline: Backend-Tests (Java 21), Frontend-Tests (Node 22), Docker-Build-Verify |
| `monitoring/prometheus.yml` | Prometheus-Konfiguration |
| `monitoring/grafana/...` | Grafana-Dashboards und Provisioning |

---

## 2. Docker-Analyse

### 2.1 Backend Dockerfile (`backend/Dockerfile`)

**Stärken:**
- Multi-Stage-Build korrekt implementiert (build → runtime)
- Schlankes Runtime-Image (`eclipse-temurin:21-jre-alpine`)
- Non-Root-User (`monteweb`) korrekt konfiguriert
- Health-Check vorhanden (wget auf Actuator)
- Dependency-Caching: `pom.xml` wird vor `src/` kopiert → Layer-Cache für Dependencies

**Probleme:**

| Schweregrad | Problem | Beschreibung |
|---|---|---|
| Mittel | Keine `.dockerignore` | Unnötige Dateien (`.git`, `target/`, IDE-Dateien, Tests) werden in den Build-Kontext kopiert → langsamer Build |
| Mittel | Kein `--no-transfer-progress` bei Maven | Maven-Output enthält Download-Fortschritt → unnötig große Build-Logs |
| Niedrig | Kein `LABEL` für Metadaten | Keine Versions-, Maintainer- oder Build-Informationen im Image |
| Niedrig | `COPY --from=build /app/target/*.jar app.jar` | Wildcard-Copy könnte bei mehreren JARs unvorhersehbar sein; besser: `-Dspring-boot.build-image.name` oder expliziter JAR-Name |
| Niedrig | Health-Check `start_period` nur im Dockerfile (30s) | In docker-compose.yml korrekt auf 60s erhöht — kein echtes Problem, aber Inkonsistenz |

### 2.2 Frontend Dockerfile (`frontend/Dockerfile`)

**Stärken:**
- Multi-Stage-Build (Node build → nginx serve)
- `npm ci` statt `npm install` für deterministische Builds
- Schlankes nginx-alpine Runtime-Image
- Health-Check vorhanden

**Probleme:**

| Schweregrad | Problem | Beschreibung |
|---|---|---|
| Mittel | Keine `.dockerignore` | `node_modules/`, `.git/`, `coverage/`, Tests, IDE-Dateien werden in Build-Kontext kopiert → sehr langsam (~400MB+ an `node_modules` werden unnötig übertragen) |
| Mittel | `COPY . .` kopiert alles | Wegen fehlender `.dockerignore` gelangen Test-Dateien, Configs, etc. in den Build-Container |
| Mittel | nginx läuft als Root | Kein User-Wechsel im Runtime-Stage; nginx-Prozess läuft als Root |
| Niedrig | Kein `LABEL` für Metadaten | Keine Versions-/Build-Informationen |
| Niedrig | Health-Check ohne `start_period` | Könnte bei langsamen Starts falsch-negative Checks liefern |

### 2.3 nginx.conf

**Stärken:**
- Gzip-Kompression konfiguriert mit sinnvollen MIME-Types
- Static-Asset-Caching mit `immutable` + 1 Jahr Expiry
- API-Proxy mit korrekten Header-Forwarding (`X-Real-IP`, `X-Forwarded-For`, `X-Forwarded-Proto`)
- WebSocket-Proxy korrekt konfiguriert
- SPA-Fallback mit `try_files`

**Probleme:**

| Schweregrad | Problem | Beschreibung |
|---|---|---|
| Kritisch | Keine Security-Header | Fehlend: `X-Frame-Options`, `X-Content-Type-Options`, `Content-Security-Policy`, `Referrer-Policy`, `Permissions-Policy`, `Strict-Transport-Security`. Anwendung ist anfällig für Clickjacking, MIME-Sniffing und andere Angriffe |
| Mittel | Actuator-Endpunkte öffentlich erreichbar | `/actuator/` wird ungefiltert nach außen proxied — `health`, `info`, `prometheus`, `metrics` sind von überall abrufbar. Mindestens `prometheus` und `metrics` sollten nicht öffentlich sein |
| Mittel | Kein `client_max_body_size` | Standard ist 1MB — bei Foto-Uploads (Fotobox, Chat-Bilder, Fundgrube) werden größere Dateien mit `413 Entity Too Large` abgewiesen |
| Niedrig | Kein `proxy_read_timeout` für WebSocket | WebSocket-Verbindungen könnten bei Inaktivität nach 60s (nginx-Standard) getrennt werden |
| Niedrig | Kein `server_tokens off` | nginx-Version wird in Fehlerseiten und Header offengelegt |

### 2.4 docker-compose.yml (Produktion)

**Stärken:**
- Health-Checks auf allen Services
- `depends_on` mit `condition: service_healthy` — korrekte Startup-Reihenfolge
- `restart: unless-stopped` auf allen Services
- Secrets als Required-Variablen (`${VAR:?error}`)
- Monitoring als optionales Profil (`--profile monitoring`)
- Named Volumes für Persistenz

**Probleme:**

| Schweregrad | Problem | Beschreibung |
|---|---|---|
| Mittel | MinIO: `minio/minio:latest` | `latest`-Tag ist nicht reproduzierbar; sollte auf eine spezifische Version gepinnt werden |
| Mittel | Keine Ressourcen-Limits | Kein `mem_limit`, `cpus`, `pids_limit` — ein einzelner Service kann die gesamten Host-Ressourcen beanspruchen |
| Mittel | Backend-Port nicht exponiert aber auch nicht eingeschränkt | Backend (8080) ist korrekt nicht auf Host gemappt — gut. Aber es gibt kein explizites internes Netzwerk; alle Container sind im selben Default-Netzwerk |
| Niedrig | Kein Netzwerk-Isolation | Alle Services teilen das Default-Netzwerk; idealerweise gibt es ein `frontend`-Netzwerk (nginx↔backend) und ein `backend`-Netzwerk (backend↔postgres/redis/minio) |
| Niedrig | Redis-Passwort im Health-Check sichtbar | `redis-cli -a ${REDIS_PASSWORD} ping` — Passwort in Prozessliste sichtbar; kein echtes Risiko in Container-Umgebung, aber unsauber |
| Niedrig | Monitoring-Images könnten aktueller sein | Prometheus v2.51.0 und Grafana 10.4.1 — nicht die neuesten Versionen |

### 2.5 docker-compose.dev.yml (Entwicklung)

**Stärken:**
- Getrennte Dev-Ports (5433, 6380) vermeiden Konflikte mit lokal laufenden Services
- Nur Infrastruktur-Services — Backend/Frontend nicht enthalten (lokal gestartet)
- Health-Checks vorhanden

**Probleme:**

| Schweregrad | Problem | Beschreibung |
|---|---|---|
| Niedrig | Default-Passwort `changeme` im YAML | Für Entwicklung akzeptabel, aber sollte klar als Dev-only dokumentiert sein |
| Niedrig | MinIO: `minio/minio:latest` | Wie in Produktion — unpinned |
| Niedrig | Kein `restart: unless-stopped` | Dev-Container starten nach Reboot nicht automatisch (für Entwicklung ggf. gewünscht) |

---

## 3. CI/CD-Pipeline-Analyse

### Workflow: `.github/workflows/ci.yml`

**Stärken:**
- Minimale Permissions (`contents: read`) — Principle of Least Privilege
- Getrennte Jobs für Backend/Frontend — parallel ausgeführt
- Dependency-Caching für Maven und npm
- Docker-Build-Job nur auf `main` (nicht bei PRs) — spart Ressourcen
- Service-Container für Backend-Tests (Postgres, Redis)
- `npm run build` (inkl. `vue-tsc`) als Type-Check
- Coverage-Report bei Tests

**Probleme:**

| Schweregrad | Problem | Beschreibung |
|---|---|---|
| Kritisch | Actions nicht auf SHA gepinnt | `actions/checkout@v4`, `actions/setup-java@v4`, `actions/setup-node@v4` — Tags können überschrieben werden; SHA-Pinning verhindert Supply-Chain-Angriffe |
| Mittel | Kein Timeout auf Jobs | Standard-Timeout ist 6 Stunden; ein hängender Test/Build würde Minuten verschwenden |
| Mittel | Kein Concurrency-Group | Doppelte Runs bei schnellen Push-Folgen; ein neuer Push sollte den alten Run abbrechen |
| Mittel | Kein Pfadfilter | Backend-Tests laufen auch bei reinen Frontend-Änderungen und umgekehrt → verschwendete CI-Minuten |
| Mittel | Docker-Job baut ohne Layer-Caching | `docker build` ohne Caching; jeder CI-Run baut komplett von Null → langsam |
| Mittel | Keine Container-Image-Security-Scans | Keine Trivy/Grype/Snyk-Scans der gebauten Images — Schwachstellen in Base-Images bleiben unentdeckt |
| Mittel | Kein Dependency-Update-Mechanismus | Weder Dependabot noch Renovate konfiguriert — veraltete Dependencies bleiben unbemerkt |
| Niedrig | `docker compose config -q` nur auf main | Compose-Konfigurationsfehler werden erst bei Merge erkannt, nicht bei PRs |
| Niedrig | Kein Linting-Job | Kein ESLint/Checkstyle/SpotBugs-Job — Code-Qualität wird nur über Tests sichergestellt |
| Niedrig | Redis-Service ohne Passwort in CI | CI-Redis läuft ohne `--requirepass` — weicht von Produktion ab. Tests könnten Verbindungsprobleme nicht erkennen |

---

## 4. Sicherheitsrisiken

| # | Bereich | Risiko | Schweregrad |
|---|---|---|---|
| S1 | nginx | Keine Security-Header (CSP, X-Frame-Options, HSTS, etc.) | Kritisch |
| S2 | CI/CD | Actions nicht SHA-gepinnt — Supply-Chain-Angriff möglich | Kritisch |
| S3 | nginx | Actuator-Endpunkte (`/actuator/metrics`, `/actuator/prometheus`) öffentlich | Mittel |
| S4 | Frontend | nginx läuft als Root-User | Mittel |
| S5 | Docker | Kein Image-Security-Scanning in CI | Mittel |
| S6 | nginx | `server_tokens` nicht deaktiviert — nginx-Version offengelegt | Niedrig |
| S7 | Docker | MinIO unpinned (`latest`) — unkontrollierte Updates | Niedrig |

---

## 5. Performance-Bottlenecks

| # | Bereich | Problem | Auswirkung |
|---|---|---|---|
| P1 | Docker Build | Fehlende `.dockerignore` (Backend + Frontend) | Build-Kontext unnötig groß (~500MB+); langsame Builds |
| P2 | CI/CD | Kein Docker-Layer-Caching in CI | Jeder CI-Run baut Docker-Images von Null (~5-10 Min extra) |
| P3 | CI/CD | Kein Pfadfilter | Alle Jobs laufen bei jeder Änderung, auch irrelevante |
| P4 | nginx | Kein `client_max_body_size` | Upload-Fehler bei >1MB Dateien (Fotos, Fotobox) |
| P5 | Docker | Kein Ressourcen-Limit | Ein Service kann alle Host-Ressourcen beanspruchen |

---

## 6. Best-Practice-Abweichungen

| # | Bereich | Abweichung | Empfehlung |
|---|---|---|---|
| B1 | Docker | Keine `.dockerignore`-Dateien | Erstellen für Backend und Frontend |
| B2 | Docker | Keine `LABEL`-Metadaten in Images | OCI-Labels hinzufügen (version, maintainer, build-date) |
| B3 | Docker | Kein Netzwerk-Isolation in Compose | Getrennte Netzwerke für Frontend/Backend und Backend/DB |
| B4 | CI/CD | Kein Dependabot/Renovate | Automatische Dependency-Updates einrichten |
| B5 | CI/CD | Kein Job-Timeout | Timeouts auf 15-20 Minuten setzen |
| B6 | CI/CD | Kein Concurrency-Management | Concurrency-Groups mit `cancel-in-progress` |
| B7 | nginx | Kein Rate-Limiting | Upstream-Level Rate-Limiting für API-Proxy |
| B8 | Docker | `minio:latest` statt gepinnter Version | Auf spezifische Version pinnen |
| B9 | Frontend | nginx als Root | Auf unprivilegierten nginx-User wechseln |

---

## 7. Zusammenfassung

### Gesamtbewertung

Die Docker-Infrastruktur ist **solide aufgebaut** mit korrekten Multi-Stage-Builds, Health-Checks, und sinnvoller Service-Orchestrierung. Die CI-Pipeline deckt die wichtigsten Aspekte (Compile, Test, Build-Verify) ab.

**Haupthandlungsfelder:**
1. **Sicherheit**: nginx Security-Header fehlen komplett — höchste Priorität
2. **Supply-Chain**: CI Actions auf SHA pinnen, Dependabot einrichten
3. **Performance**: `.dockerignore` erstellen, CI-Caching und Pfadfilter optimieren
4. **Härtung**: nginx als Non-Root, Actuator-Endpoints einschränken, Netzwerk-Isolation
5. **Observability**: Container-Image-Scanning, Upload-Limits, Timeouts

### Prioritäten-Matrix

```
Kritisch (sofort):      S1 (Security-Header), S2 (SHA-Pinning)
Mittel (kurzfristig):   P1 (.dockerignore), S3 (Actuator), P4 (Upload-Limit),
                        S4 (nginx Root), S5 (Image-Scan), B4 (Dependabot),
                        P2 (CI-Caching), P3 (Pfadfilter)
Niedrig (Verbesserung): B2 (Labels), B3 (Netzwerke), B5/B6 (Timeouts/Concurrency),
                        S6 (server_tokens), B8 (MinIO-Pin)
```
