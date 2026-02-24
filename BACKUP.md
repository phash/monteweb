# MonteWeb Backup & Restore

Automatisierte Backup-Strategie fuer PostgreSQL, MinIO und Redis.

## Schnellstart

```bash
# 1. Backup-Service aktivieren
docker compose --profile backup up -d

# 2. Sofort ein Backup erstellen (ohne auf Cron zu warten)
docker compose exec backup backup.sh

# 3. Backups auflisten
docker compose exec backup restore.sh --list

# 4. Wiederherstellen
docker compose exec backup restore.sh latest
docker compose restart backend
```

## Was wird gesichert?

| Datenspeicher | Methode | Inhalt |
|---|---|---|
| **PostgreSQL** | `pg_dump --format=custom` | Alle Daten: Benutzer, Familien, Posts, Konfiguration, etc. |
| **MinIO** | `mc mirror` | Alle Dateien: Fotos, Dokumente, Avatare, Chat-Bilder, Fotobox |
| **Redis** | `BGSAVE` (RDB) | Sessions und Cache (niedrige Prioritaet, wird automatisch neu aufgebaut) |
| **Solr** | Kein Backup | Suchindex kann jederzeit ueber Admin-UI oder API neu erstellt werden |

## Konfiguration

Alle Einstellungen werden ueber Umgebungsvariablen in `.env` gesteuert:

```env
# Cron-Schedule (Standard: taeglich um 02:00 Uhr)
BACKUP_CRON=0 2 * * *

# Aufbewahrung
BACKUP_RETENTION_DAILY=7     # 7 taegliche Backups behalten
BACKUP_RETENTION_WEEKLY=4    # 4 woechentliche Backups behalten (Sonntags)
BACKUP_RETENTION_MONTHLY=3   # 3 monatliche Backups behalten (1. des Monats)

# Initiales Backup beim Start des Containers
BACKUP_RUN_ON_START=false

# Zeitzone (wichtig fuer Cron-Schedule)
TZ=Europe/Berlin

# Optional: Externes S3-kompatibles Backup-Ziel
BACKUP_S3_ENDPOINT=https://s3.example.com
BACKUP_S3_BUCKET=monteweb-backups
BACKUP_S3_ACCESS_KEY=...
BACKUP_S3_SECRET_KEY=...
```

## Backup-Rotation

| Typ | Zeitpunkt | Aufbewahrung | Beispiel |
|-----|----------|-------------|---------|
| Taeglich | Jede Nacht 02:00 | 7 Stueck | Mo–So der letzten Woche |
| Woechentlich | Sonntag | 4 Stueck | Letzten 4 Sonntage |
| Monatlich | 1. des Monats | 3 Stueck | Letzten 3 Monate |

Aeltere Backups werden automatisch geloescht.

## Manuell ein Backup erstellen

```bash
docker compose exec backup backup.sh
```

## Wiederherstellen

### Verfuegbare Backups anzeigen

```bash
docker compose exec backup restore.sh --list
```

### Letztes Backup wiederherstellen

```bash
# Alles (Datenbank + Dateien)
docker compose exec backup restore.sh latest

# Nur Datenbank
docker compose exec backup restore.sh --db-only latest

# Nur MinIO-Dateien
docker compose exec backup restore.sh --files-only latest

# Bestimmtes Datum
docker compose exec backup restore.sh 2026-02-24

# Ohne Bestaetigung (fuer Automatisierung)
docker compose exec backup restore.sh --yes latest
```

### Nach der Wiederherstellung

```bash
# Backend neu starten (laedt neue Daten)
docker compose restart backend

# Optional: Solr-Index neu aufbauen
curl -X POST http://localhost/api/v1/admin/search/reindex \
  -H "Authorization: Bearer <admin-token>"
```

## Backup-Speicherort

Backups werden im Docker Volume `backup_data` gespeichert:

```
/backups/
├── daily/
│   ├── monteweb_2026-02-24_02-00-00.tar.gz
│   ├── monteweb_2026-02-23_02-00-00.tar.gz
│   └── ...
├── weekly/
│   ├── monteweb_2026-02-23_02-00-00.tar.gz
│   └── ...
└── monthly/
    ├── monteweb_2026-02-01_02-00-00.tar.gz
    └── ...
```

Jedes Archiv enthaelt:
- `db_TIMESTAMP.dump` — PostgreSQL-Dump (komprimiert, custom format)
- `minio_TIMESTAMP/` — Alle MinIO-Dateien

### Backup-Volume auf Host mounten

Fuer einfacheren Zugriff kann das Volume auf einen Host-Pfad gemountet werden. Dazu in `docker-compose.yml` aendern:

```yaml
backup:
  volumes:
    - /pfad/auf/host/backups:/backups  # statt backup_data:/backups
```

## Externes S3-Backup

Backups koennen automatisch zu einem S3-kompatiblen Speicher hochgeladen werden (z.B. AWS S3, Hetzner Object Storage, Backblaze B2, MinIO auf anderem Server):

```env
BACKUP_S3_ENDPOINT=https://s3.eu-central-1.amazonaws.com
BACKUP_S3_BUCKET=monteweb-backups
BACKUP_S3_ACCESS_KEY=AKIA...
BACKUP_S3_SECRET_KEY=...
```

## Monitoring

### Backup-Logs pruefen

```bash
docker compose logs backup --tail 50
```

### Letztes Backup pruefen

```bash
docker compose exec backup ls -lah /backups/daily/ | tail -5
```

## Notfall-Wiederherstellung

Falls der gesamte Server verloren geht:

1. Neuen Server aufsetzen, Repository klonen
2. `.env` Datei wiederherstellen (aus sicherem Speicherort)
3. Backup-Archiv vom S3-Remote oder externen Speicher kopieren
4. Stack starten: `docker compose up -d`
5. Backup einspielen:
   ```bash
   docker compose --profile backup up -d
   docker cp /pfad/zum/backup.tar.gz monteweb-backup:/backups/daily/
   docker compose exec backup restore.sh latest
   docker compose restart backend
   ```
6. Solr-Index neu aufbauen (Admin-UI oder API)

## Empfehlungen fuer Produktion

- **S3-Remote aktivieren** — Lokale Backups schuetzen nicht bei Server-Totalausfall
- **Backup-Volume auf externen Speicher mounten** — z.B. NFS, NAS
- **Regelmaessig Restore testen** — Mindestens 1x pro Quartal
- **`.env` Datei sicher aufbewahren** — Enthaelt alle Passwoerter und Secrets
- **Monitoring einrichten** — Backup-Logs ueberwachen, bei Fehlern alarmieren
