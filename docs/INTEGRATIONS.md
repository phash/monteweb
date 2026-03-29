# Integrations & Optional Services

## Caddy (SSL/HTTPS)

Reverse Proxy mit automatischem Let's Encrypt. Aktivierung via Docker Compose Profil:

```bash
docker compose --profile ssl up -d
```

**Konfiguration** (`Caddyfile`):
- `DOMAIN` Environment-Variable steuert das Verhalten:
  - `localhost` → selbstsigniertes Zertifikat (Entwicklung)
  - `:80` → kein SSL (LAN-Zugriff)
  - `monteweb.deineschule.de` → automatisches Let's Encrypt (Produktion)
- Voraussetzung fuer Let's Encrypt: Port 80+443 offen, DNS zeigt auf Server
- Security Headers: X-Frame-Options, X-Content-Type-Options, Referrer-Policy, Permissions-Policy
- Request Body Limit: 50MB

```env
# .env fuer SSL:
APP_PORT=          # leer lassen! Caddy uebernimmt
DOMAIN=monteweb.deineschule.de
FRONTEND_URL=https://monteweb.deineschule.de
```

## Cloudflare Quick Tunnel

Fuer schnellen externen Zugriff ohne eigene Domain/SSL:

```bash
./scripts/deploy.sh --new-tunnel     # Startet Tunnel, zeigt URL
./scripts/deploy.sh --status         # Zeigt aktuelle Tunnel-URL
```

- Installiert `cloudflared` automatisch falls noetig
- URL wird in `.tunnel-url` gespeichert
- **WICHTIG:** `FRONTEND_URL` in `.env` MUSS zur Tunnel-URL passen, sonst CORS-Fehler (403 "Invalid CORS request")
- Tunnel-URL aendert sich bei jedem Neustart (Quick Tunnel)

## E-Mail (SMTP)

```env
EMAIL_ENABLED=true
EMAIL_FROM=noreply@monteweb.deineschule.de
SMTP_HOST=smtp.example.com
SMTP_PORT=587
SMTP_USERNAME=user
SMTP_PASSWORD=pass
SMTP_AUTH=true
SMTP_STARTTLS=true
```

Verwendet fuer: Password-Reset-E-Mails, Einladungen. Wenn deaktiviert, werden Token nur geloggt (nicht per E-Mail versendet).

## OIDC / SSO

```env
OIDC_ENABLED=true
OIDC_PROVIDER_NAME=google       # oder azure, keycloak, etc.
OIDC_CLIENT_ID=xxx
OIDC_CLIENT_SECRET=xxx
OIDC_ISSUER_URI=https://accounts.google.com
```

- Unterstuetzt jeden OpenID Connect Provider
- One-Time-Code-Pattern: OIDC-Flow nutzt Redis-backed Codes statt direkter Token-Weitergabe
- Bestehende User werden automatisch per E-Mail verknuepft (`linkOidcProvider`)

## Web Push Notifications (VAPID)

```env
PUSH_ENABLED=true
VAPID_PUBLIC_KEY=<generiert mit: npx web-push generate-vapid-keys>
VAPID_PRIVATE_KEY=<generiert>
VAPID_SUBJECT=mailto:admin@monteweb.deineschule.de
```

Push-Payloads werden ueber Browser-Vendor-Server geroutet (Google/Mozilla/Apple).

## LDAP / Active Directory

DB-managed Toggle in `tenant_config.modules`:
```sql
-- Aktivierung via Admin UI oder direkt:
UPDATE tenant_config SET modules = jsonb_set(modules, '{ldap}', 'true');
```

- `@ConditionalOnProperty` auf allen LDAP-Beans
- LDAP-Injection-Schutz via `escapeForLdap()` (RFC 4515)
- LDAP-URL-Validierung: nur `ldap://` und `ldaps://` Schemes erlaubt (SSRF-Schutz)

## Jitsi Videokonferenzen

DB-managed Toggle. Oeffnet Jitsi in neuem Tab (`window.open`), kein iframe.
- Konfigurierbar via Admin UI: `jitsiServerUrl` in Tenant Config
- Verfuegbar in Kalender-Events und Raum-Chats

## ONLYOFFICE (WOPI)

```bash
docker compose --profile office up -d
```

- Document Server auf Port 8443
- JWT-Secret MUSS konfiguriert sein (PutFile wird ohne JWT-Secret abgelehnt)
- iframe mit `sandbox="allow-scripts allow-same-origin allow-forms allow-popups allow-popups-to-escape-sandbox"`

## ClamAV Virenscanner

```bash
docker compose --profile clamav up -d
```

- Scannt Datei-Uploads auf Viren
- **Fail-closed:** Wenn ClamAV nicht erreichbar, werden Uploads blockiert
- Memory Limit: 1G (ClamAV braucht viel RAM fuer Signaturen)

## Monitoring (Prometheus + Grafana)

```bash
docker compose --profile monitoring up -d
```

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (Default: admin/admin)
- Backend-Metriken via Spring Actuator `/actuator/prometheus`

## Solr Volltextsuche

Immer aktiv (eigener Container in docker-compose.yml).

- Solr 9.8-slim mit deutscher Sprachanalyse
- 7 Dokumenttypen: USER, ROOM, POST, EVENT, FILE, WIKI, TASK
- Tika-Extraktion fuer Dateiinhalte (PDF, DOCX, etc.)
- Admin-Reindex: `POST /api/v1/admin/search/reindex`
- Fallback auf DB-Suche wenn `monteweb.modules.solr.enabled=false`
