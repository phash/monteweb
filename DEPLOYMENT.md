# MonteWeb -- Deployment

## Voraussetzungen

- Linux-PC/Server mit [Docker](https://docs.docker.com/engine/install/) und [Docker Compose](https://docs.docker.com/compose/install/)
- Ports 80 und 443 frei (fuer Caddy SSL-Proxy)
- Fuer HTTPS: Domain mit DNS-Eintrag auf den Server (z.B. `monteweb.deineschule.de`)

## 1. Repo klonen

```bash
git clone https://github.com/phash/monteweb.git
cd monteweb
```

## 2. `.env` anpassen

```bash
cp .env .env.backup
nano .env
```

Folgende Werte anpassen:

```env
POSTGRES_PASSWORD=ein-sicheres-passwort
REDIS_PASSWORD=ein-sicheres-passwort
JWT_SECRET=<openssl rand -base64 64>
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=ein-sicheres-passwort
FRONTEND_URL=https://monteweb.deineschule.de
```

### SSL / HTTPS fuer Produktion (optional)

Fuer den Betrieb im Internet wird HTTPS empfohlen. MonteWeb bringt einen Caddy-Proxy mit, der automatisch Let's Encrypt Zertifikate holt:

```env
# In .env:
APP_PORT=          # leer lassen! Caddy uebernimmt Port 80/443
DOMAIN=monteweb.deineschule.de
FRONTEND_URL=https://monteweb.deineschule.de
```

```bash
# Mit SSL starten:
docker compose --profile ssl up -d
```

Caddy holt sich automatisch ein TLS-Zertifikat von Let's Encrypt. Voraussetzung: Port 80 und 443 muessen vom Internet erreichbar sein und der DNS-Eintrag muss auf den Server zeigen.

## 3. Starten

```bash
docker compose up -d --build
```

Der erste Build dauert einige Minuten (Maven-Dependencies + npm install).

## 4. Zugriff

| Modus | URL |
|-------|-----|
| Lokal | `http://localhost` |
| LAN | `http://<IP-des-Servers>` |
| Produktion | `https://monteweb.deineschule.de` (automatisches SSL) |

> Die IP des Servers findest du mit `hostname -I | awk '{print $1}'`

### Test-Accounts

| Rolle | E-Mail | Passwort |
|-------|--------|----------|
| Superadmin | `admin@monteweb.local` | `test1234` |
| Section-Admin | `sectionadmin@monteweb.local` | `test1234` |
| Lehrer | `lehrer@monteweb.local` | `test1234` |
| Eltern | `eltern@monteweb.local` | `test1234` |
| Schueler | `schueler@monteweb.local` | `test1234` |

## Architektur

```
Internet --HTTPS--> Caddy :443 --HTTP--> nginx :80 --proxy--> Backend :8080
                    (Let's Encrypt)      (SPA + API)          (Spring Boot)
```

5 Container: `frontend` (nginx), `backend` (Java), `postgres`, `redis`, `minio`
Mit SSL-Profil: + `caddy` (6 Container)

## Nuetzliche Befehle

```bash
# Status pruefen
docker compose ps

# Logs anzeigen
docker compose logs backend --tail=100
docker compose logs caddy --tail=50
docker compose logs backend -f          # live

# Neustart nach Code-Aenderungen
git pull
docker compose up -d --build

# Alles stoppen
docker compose down

# Alles stoppen + Daten loeschen
docker compose down -v

# SSL-Zertifikate pruefen (Caddy)
docker compose exec caddy caddy list-modules
docker compose logs caddy | grep -i "tls\|certificate"

# Monitoring (optional)
docker compose --profile monitoring up -d
```
