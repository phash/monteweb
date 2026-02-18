# MonteWeb — Deployment (Test-Instanz)

Einfaches Deployment auf einem Linux-PC im lokalen Netzwerk.

## Voraussetzungen

- Linux-PC mit [Docker](https://docs.docker.com/engine/install/) und [Docker Compose](https://docs.docker.com/compose/install/)
- Ports 80 frei

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
FRONTEND_URL=http://<IP-des-PCs>
```

> Die IP des PCs findest du mit `hostname -I | awk '{print $1}'`

## 3. Starten

```bash
docker compose up -d --build
```

Der erste Build dauert einige Minuten (Maven-Dependencies + npm install).

## 4. Zugriff

Die App ist erreichbar unter `http://<IP-des-PCs>` von jedem Geraet im selben Netzwerk.

### Test-Accounts

| Rolle | E-Mail | Passwort |
|-------|--------|----------|
| Superadmin | `admin@monteweb.local` | `test1234` |
| Section-Admin | `sectionadmin@monteweb.local` | `test1234` |
| Lehrer | `lehrer@monteweb.local` | `test1234` |
| Eltern | `eltern@monteweb.local` | `test1234` |
| Schueler | `schueler@monteweb.local` | `test1234` |

## Nuetzliche Befehle

```bash
# Status pruefen
docker compose ps

# Logs anzeigen
docker compose logs backend --tail=100
docker compose logs backend -f          # live

# Neustart nach Code-Aenderungen
git pull
docker compose up -d --build

# Alles stoppen
docker compose down

# Alles stoppen + Daten loeschen
docker compose down -v
```
