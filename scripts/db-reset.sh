#!/usr/bin/env bash
# ============================================================
# MonteWeb — Datenbank-Reset
# Löscht ALLE Inhalte und setzt den Tenant auf Bayern zurück.
# Der Admin-User wird neu angelegt (Passwort: test1234).
#
# Verwendung:
#   ./scripts/db-reset.sh              # Docker-Modus (monteweb-postgres Container)
#   ./scripts/db-reset.sh --local      # Lokal (docker-compose.dev.yml, Port 5433)
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODE="docker"
[[ "${1:-}" == "--local" ]] && MODE="local"

DB_NAME="${POSTGRES_DB:-monteweb}"
DB_USER="${POSTGRES_USER:-monteweb}"
DB_PASS="${POSTGRES_PASSWORD:-changeme}"

echo "============================================================"
echo " MonteWeb DB-Reset  (Modus: $MODE)"
echo "============================================================"

run_psql() {
  if [[ "$MODE" == "local" ]]; then
    PGPASSWORD="$DB_PASS" psql -h localhost -p 5433 -U "$DB_USER" -d "$DB_NAME" "$@"
  else
    docker exec -i monteweb-postgres psql -U "$DB_USER" -d "$DB_NAME" "$@"
  fi
}

# Verbindungstest
echo "→ Verbindung prüfen..."
run_psql -c "SELECT 1" > /dev/null

echo "→ Alle Tabellen leeren..."
run_psql <<'SQL'
-- Alle Tabellen außer flyway_schema_history dynamisch ermitteln und truncaten
DO $$
DECLARE
  _tables text;
BEGIN
  SELECT string_agg(quote_ident(tablename), ', ' ORDER BY tablename)
  INTO _tables
  FROM pg_tables
  WHERE schemaname = 'public'
    AND tablename <> 'flyway_schema_history';

  IF _tables IS NOT NULL THEN
    EXECUTE 'TRUNCATE TABLE ' || _tables || ' CASCADE';
    RAISE NOTICE 'Geleert: %', _tables;
  END IF;
END $$;
SQL

echo "→ Tenant-Konfiguration (Bayern) wiederherstellen..."
run_psql <<'SQL'
INSERT INTO tenant_config (
  school_name,
  theme,
  bundesland,
  modules,
  available_languages,
  default_language,
  multilanguage_enabled,
  require_user_approval,
  require_assignment_confirmation,
  max_upload_size_mb,
  privacy_policy_text,
  privacy_policy_version,
  terms_text,
  terms_version,
  data_retention_days_notifications,
  data_retention_days_audit
) VALUES (
  'Montessori Schule München',
  '{"primaryColor": "#2E7D32", "secondaryColor": "#FF8F00", "fontFamily": "Inter, sans-serif"}',
  'BY',
  '{
    "messaging": true, "files": true, "jobboard": true, "cleaning": true,
    "calendar": true, "forms": true, "fotobox": true, "fundgrube": true,
    "bookmarks": true, "tasks": true, "wiki": true, "profilefields": false,
    "jitsi": false, "wopi": false, "clamav": false,
    "maintenance": false, "ldap": false, "directoryAdminOnly": false
  }',
  ARRAY['de','en'],
  'de',
  true,
  true,
  true,
  50,
  'Die Montessori Schule München verarbeitet Ihre personenbezogenen Daten gemäß der EU-Datenschutzgrundverordnung (DSGVO).',
  '1.0',
  'Mit der Nutzung dieser Plattform akzeptieren Sie die Nutzungsbedingungen der Montessori Schule München.',
  '1.0',
  90,
  365
);
SQL

echo "→ Admin-User anlegen (admin@monteweb.local / test1234)..."
run_psql <<'SQL'
-- bcrypt-Hash für "test1234"
INSERT INTO users (
  id, email, password_hash,
  first_name, last_name, display_name,
  role, is_active, email_verified
) VALUES (
  gen_random_uuid(),
  'admin@monteweb.local',
  '$2a$10$dQa6248nzH.S5yYnSpGfr.b7BnuGroDZ/G18yp.vzoAx45FQ7hecO',
  'Admin', 'MonteWeb', 'Admin MonteWeb',
  'SUPERADMIN', true, true
);
SQL

echo ""
echo "✅ Reset abgeschlossen."
echo "   Admin-Login: admin@monteweb.local / test1234"
echo "   → Jetzt './scripts/db-seed.sh' ausführen (oder: db-seed.sh --reset fuer alles in einem)"
