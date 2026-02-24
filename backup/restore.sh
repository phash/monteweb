#!/bin/bash
# =============================================================================
# MonteWeb Restore Script
# Restores PostgreSQL database and/or MinIO files from a backup archive.
#
# Usage:
#   restore.sh latest                     # restore latest daily backup
#   restore.sh 2026-02-24                 # restore backup from specific date
#   restore.sh --db-only latest           # restore only database
#   restore.sh --files-only latest        # restore only MinIO files
#   restore.sh --list                     # list available backups
#   restore.sh /path/to/archive.tar.gz    # restore from specific archive
# =============================================================================
set -euo pipefail

# --- Configuration (from environment) ----------------------------------------
PGHOST="${PGHOST:-postgres}"
PGPORT="${PGPORT:-5432}"
PGUSER="${PGUSER:-monteweb}"
PGPASSWORD="${PGPASSWORD:?PGPASSWORD is required}"
PGDATABASE="${PGDATABASE:-monteweb}"

MINIO_ENDPOINT="${MINIO_ENDPOINT:-http://minio:9000}"
MINIO_ACCESS_KEY="${MINIO_ACCESS_KEY:?MINIO_ACCESS_KEY is required}"
MINIO_SECRET_KEY="${MINIO_SECRET_KEY:?MINIO_SECRET_KEY is required}"
MINIO_BUCKET="${MINIO_BUCKET:-monteweb}"

BACKUP_DIR="/backups"
RESTORE_DB=true
RESTORE_FILES=true

# --- Parse arguments ----------------------------------------------------------

usage() {
    echo "Usage: restore.sh [OPTIONS] TARGET"
    echo ""
    echo "TARGET:"
    echo "  latest           Restore the most recent daily backup"
    echo "  YYYY-MM-DD       Restore backup from a specific date"
    echo "  /path/to/file    Restore from a specific archive file"
    echo "  --list           List all available backups"
    echo ""
    echo "OPTIONS:"
    echo "  --db-only        Restore only the PostgreSQL database"
    echo "  --files-only     Restore only MinIO files"
    echo "  --yes            Skip confirmation prompt"
    echo ""
    exit 1
}

SKIP_CONFIRM=false
TARGET=""

while [ $# -gt 0 ]; do
    case "$1" in
        --db-only)
            RESTORE_DB=true
            RESTORE_FILES=false
            shift
            ;;
        --files-only)
            RESTORE_DB=false
            RESTORE_FILES=true
            shift
            ;;
        --yes)
            SKIP_CONFIRM=true
            shift
            ;;
        --list)
            echo "=== Available Backups ==="
            echo ""
            echo "--- Daily ---"
            find "${BACKUP_DIR}/daily" -name "monteweb_*.tar.gz" -type f 2>/dev/null | sort -r | while read -r f; do
                echo "  $(basename "${f}")  ($(du -sh "${f}" | cut -f1))"
            done
            echo ""
            echo "--- Weekly ---"
            find "${BACKUP_DIR}/weekly" -name "monteweb_*.tar.gz" -type f 2>/dev/null | sort -r | while read -r f; do
                echo "  $(basename "${f}")  ($(du -sh "${f}" | cut -f1))"
            done
            echo ""
            echo "--- Monthly ---"
            find "${BACKUP_DIR}/monthly" -name "monteweb_*.tar.gz" -type f 2>/dev/null | sort -r | while read -r f; do
                echo "  $(basename "${f}")  ($(du -sh "${f}" | cut -f1))"
            done
            exit 0
            ;;
        --help|-h)
            usage
            ;;
        *)
            TARGET="$1"
            shift
            ;;
    esac
done

if [ -z "${TARGET}" ]; then
    usage
fi

# --- Resolve archive file -----------------------------------------------------

ARCHIVE=""

if [ "${TARGET}" = "latest" ]; then
    ARCHIVE=$(find "${BACKUP_DIR}/daily" -name "monteweb_*.tar.gz" -type f 2>/dev/null | sort -r | head -1)
    if [ -z "${ARCHIVE}" ]; then
        echo "ERROR: No daily backups found in ${BACKUP_DIR}/daily/"
        exit 1
    fi
elif [ -f "${TARGET}" ]; then
    ARCHIVE="${TARGET}"
else
    # Try to find backup matching the date pattern
    ARCHIVE=$(find "${BACKUP_DIR}" -name "monteweb_${TARGET}*.tar.gz" -type f 2>/dev/null | sort -r | head -1)
    if [ -z "${ARCHIVE}" ]; then
        echo "ERROR: No backup found for '${TARGET}'"
        echo "Use --list to see available backups"
        exit 1
    fi
fi

echo "=== MonteWeb Restore ==="
echo "Archive:   ${ARCHIVE}"
echo "Size:      $(du -sh "${ARCHIVE}" | cut -f1)"
echo "Database:  $([ "${RESTORE_DB}" = true ] && echo "YES" || echo "SKIP")"
echo "Files:     $([ "${RESTORE_FILES}" = true ] && echo "YES" || echo "SKIP")"
echo "========================="

if [ "${SKIP_CONFIRM}" != "true" ]; then
    echo ""
    echo "WARNING: This will overwrite current data!"
    echo -n "Continue? [y/N] "
    read -r confirm
    if [ "${confirm}" != "y" ] && [ "${confirm}" != "Y" ]; then
        echo "Aborted."
        exit 0
    fi
fi

# --- Extract archive ----------------------------------------------------------

TEMP_DIR=$(mktemp -d)
trap 'rm -rf ${TEMP_DIR}' EXIT

echo "Extracting archive..."
tar xzf "${ARCHIVE}" -C "${TEMP_DIR}"

# --- Restore PostgreSQL -------------------------------------------------------

if [ "${RESTORE_DB}" = true ]; then
    DUMP_FILE=$(find "${TEMP_DIR}" -name "db_*.dump" -type f | head -1)
    if [ -z "${DUMP_FILE}" ]; then
        echo "WARNING: No database dump found in archive"
    else
        echo "Restoring PostgreSQL database..."
        export PGPASSWORD

        # Drop and recreate the database
        echo "  Dropping existing database..."
        psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d postgres \
            -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='${PGDATABASE}' AND pid <> pg_backend_pid();" \
            2>/dev/null || true
        psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d postgres \
            -c "DROP DATABASE IF EXISTS ${PGDATABASE};" 2>/dev/null || true
        psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d postgres \
            -c "CREATE DATABASE ${PGDATABASE} OWNER ${PGUSER};" 2>/dev/null

        echo "  Restoring from dump..."
        pg_restore -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d "${PGDATABASE}" \
            --no-owner --no-privileges --clean --if-exists \
            "${DUMP_FILE}" 2>/dev/null || true

        echo "  PostgreSQL restore complete"
    fi
fi

# --- Restore MinIO ------------------------------------------------------------

if [ "${RESTORE_FILES}" = true ]; then
    MINIO_DIR=$(find "${TEMP_DIR}" -maxdepth 1 -name "minio_*" -type d | head -1)
    if [ -z "${MINIO_DIR}" ]; then
        echo "WARNING: No MinIO backup found in archive"
    else
        echo "Restoring MinIO files..."

        mc alias set restore "${MINIO_ENDPOINT}" "${MINIO_ACCESS_KEY}" "${MINIO_SECRET_KEY}" --quiet 2>/dev/null || true

        # Ensure bucket exists
        mc mb "restore/${MINIO_BUCKET}" --ignore-existing --quiet 2>/dev/null || true

        mc mirror --overwrite --quiet "${MINIO_DIR}/" "restore/${MINIO_BUCKET}" 2>/dev/null || true

        echo "  MinIO restore complete"
    fi
fi

# --- Done ---------------------------------------------------------------------

echo ""
echo "=== Restore complete ==="
echo ""
echo "IMPORTANT: Restart the backend to pick up changes:"
echo "  docker compose restart backend"
echo ""
if [ "${RESTORE_DB}" = true ]; then
    echo "NOTE: Flyway will validate the schema on next startup."
    echo "If the backup is from an older version, run migrations first."
fi
