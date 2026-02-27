#!/usr/bin/env bash
# ============================================================
# MonteWeb — Lebendige Schule seeden
# Erzeugt 50 Schüler, 9 Klassen, 8 andere Räume, Kalender-Events.
# Setzt Bayern als Bundesland.
#
# Verwendung:
#   ./scripts/db-seed.sh              # Docker-Modus
#   ./scripts/db-seed.sh --local      # Lokal (Port 5433)
#   ./scripts/db-seed.sh --reset      # Erst reset, dann seed (Docker)
#   ./scripts/db-seed.sh --local --reset
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODE="docker"
DO_RESET=false

for arg in "$@"; do
  [[ "$arg" == "--local" ]]  && MODE="local"
  [[ "$arg" == "--reset" ]]  && DO_RESET=true
done

DB_NAME="${POSTGRES_DB:-monteweb}"
DB_USER="${POSTGRES_USER:-monteweb}"
DB_PASS="${POSTGRES_PASSWORD:-changeme}"

echo "============================================================"
echo " MonteWeb DB-Seed  (Modus: $MODE)"
echo "============================================================"

run_psql() {
  if [[ "$MODE" == "local" ]]; then
    PGPASSWORD="$DB_PASS" psql -h localhost -p 5433 -U "$DB_USER" -d "$DB_NAME" "$@"
  else
    docker exec -i monteweb-postgres psql -U "$DB_USER" -d "$DB_NAME" "$@"
  fi
}

# Optional: erst reset
if [[ "$DO_RESET" == "true" ]]; then
  echo "→ Führe Reset aus..."
  if [[ "$MODE" == "local" ]]; then
    bash "$SCRIPT_DIR/db-reset.sh" --local
  else
    bash "$SCRIPT_DIR/db-reset.sh"
  fi
  echo ""
fi

# Verbindungstest
echo "→ Verbindung prüfen..."
run_psql -c "SELECT 1" > /dev/null

echo "→ Seed-SQL ausführen (ca. 10-20 Sekunden)..."
run_psql < "$SCRIPT_DIR/seed-school.sql"

echo ""
echo "✅ Seed abgeschlossen!"
echo ""
echo "   Test-Accounts (alle Passwort: test1234):"
echo "   - admin@monteweb.local        (SUPERADMIN)"
echo "   - maria.huber@monteweb.local  (TEACHER, Krippe Leiterin)"
echo "   - anna.mueller@monteweb.local (PARENT, Familie Müller)"
echo "   - liam.mueller@monteweb.local (STUDENT, Krippe)"
echo ""
echo "   9 Klassen + 8 andere Räume + Kalender-Events erstellt."
