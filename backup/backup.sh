#!/bin/bash
# =============================================================================
# MonteWeb Backup Script
# Backs up PostgreSQL, MinIO files, and Redis RDB snapshot.
# Supports daily/weekly/monthly rotation and optional S3 remote upload.
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

REDIS_HOST="${REDIS_HOST:-redis}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"

RETENTION_DAILY="${BACKUP_RETENTION_DAILY:-7}"
RETENTION_WEEKLY="${BACKUP_RETENTION_WEEKLY:-4}"
RETENTION_MONTHLY="${BACKUP_RETENTION_MONTHLY:-3}"

BACKUP_S3_ENDPOINT="${BACKUP_S3_ENDPOINT:-}"
BACKUP_S3_BUCKET="${BACKUP_S3_BUCKET:-}"
BACKUP_S3_ACCESS_KEY="${BACKUP_S3_ACCESS_KEY:-}"
BACKUP_S3_SECRET_KEY="${BACKUP_S3_SECRET_KEY:-}"

BACKUP_DIR="/backups"
DATE=$(date +%Y-%m-%d)
TIMESTAMP=$(date +%Y-%m-%d_%H-%M-%S)
DAY_OF_WEEK=$(date +%u)   # 1=Monday, 7=Sunday
DAY_OF_MONTH=$(date +%d)

DAILY_DIR="${BACKUP_DIR}/daily"
WEEKLY_DIR="${BACKUP_DIR}/weekly"
MONTHLY_DIR="${BACKUP_DIR}/monthly"

LOG_PREFIX="[backup ${TIMESTAMP}]"

# --- Functions ---------------------------------------------------------------

log() {
    echo "${LOG_PREFIX} $1"
}

error() {
    echo "${LOG_PREFIX} ERROR: $1" >&2
}

# --- Pre-flight checks -------------------------------------------------------

log "Starting MonteWeb backup..."

mkdir -p "${DAILY_DIR}" "${WEEKLY_DIR}" "${MONTHLY_DIR}"

# --- 1. PostgreSQL Backup ----------------------------------------------------
log "Backing up PostgreSQL..."

export PGPASSWORD
PG_DUMP_FILE="${DAILY_DIR}/db_${TIMESTAMP}.dump"

if pg_dump -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d "${PGDATABASE}" \
    --format=custom --compress=6 --no-owner --no-privileges \
    -f "${PG_DUMP_FILE}"; then
    PG_SIZE=$(du -sh "${PG_DUMP_FILE}" | cut -f1)
    log "PostgreSQL backup complete: ${PG_DUMP_FILE} (${PG_SIZE})"
else
    error "PostgreSQL backup failed!"
    exit 1
fi

# --- 2. MinIO Backup ---------------------------------------------------------
log "Backing up MinIO files..."

MINIO_BACKUP_DIR="${DAILY_DIR}/minio_${TIMESTAMP}"
mkdir -p "${MINIO_BACKUP_DIR}"

# Configure mc alias (suppress output)
mc alias set backup "${MINIO_ENDPOINT}" "${MINIO_ACCESS_KEY}" "${MINIO_SECRET_KEY}" --quiet 2>/dev/null || true

if mc mirror --quiet backup/"${MINIO_BUCKET}" "${MINIO_BACKUP_DIR}/" 2>/dev/null; then
    MINIO_SIZE=$(du -sh "${MINIO_BACKUP_DIR}" | cut -f1)
    log "MinIO backup complete: ${MINIO_BACKUP_DIR} (${MINIO_SIZE})"
else
    # mc mirror returns non-zero if bucket is empty — treat as warning not error
    if mc ls backup/"${MINIO_BUCKET}" 2>/dev/null | head -1 | grep -q .; then
        error "MinIO backup failed!"
    else
        log "MinIO bucket is empty, skipping"
        rmdir "${MINIO_BACKUP_DIR}" 2>/dev/null || true
    fi
fi

# --- 3. Redis Backup ---------------------------------------------------------
log "Backing up Redis..."

REDIS_AUTH=""
if [ -n "${REDIS_PASSWORD}" ]; then
    REDIS_AUTH="-a ${REDIS_PASSWORD}"
fi

# Trigger BGSAVE and wait for it to complete
if redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" ${REDIS_AUTH} --no-auth-warning BGSAVE 2>/dev/null; then
    # Wait for background save to finish (max 60 seconds)
    for i in $(seq 1 60); do
        LASTSAVE=$(redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" ${REDIS_AUTH} --no-auth-warning LASTSAVE 2>/dev/null)
        sleep 1
        NEWSAVE=$(redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" ${REDIS_AUTH} --no-auth-warning LASTSAVE 2>/dev/null)
        if [ "${NEWSAVE}" != "${LASTSAVE}" ] || [ "${i}" -eq 1 ]; then
            break
        fi
    done
    log "Redis BGSAVE triggered (RDB snapshot saved inside Redis container)"
else
    log "Redis backup skipped (could not connect)"
fi

# --- 4. Create tarball of daily backup ---------------------------------------
log "Creating compressed backup archive..."

ARCHIVE_FILE="${DAILY_DIR}/monteweb_${TIMESTAMP}.tar.gz"
ARCHIVE_CONTENTS=""

# Build list of files to archive
if [ -f "${PG_DUMP_FILE}" ]; then
    ARCHIVE_CONTENTS="${ARCHIVE_CONTENTS} -C ${DAILY_DIR} $(basename ${PG_DUMP_FILE})"
fi
if [ -d "${MINIO_BACKUP_DIR}" ]; then
    ARCHIVE_CONTENTS="${ARCHIVE_CONTENTS} -C ${DAILY_DIR} $(basename ${MINIO_BACKUP_DIR})"
fi

if [ -n "${ARCHIVE_CONTENTS}" ]; then
    # shellcheck disable=SC2086
    tar czf "${ARCHIVE_FILE}" ${ARCHIVE_CONTENTS}
    ARCHIVE_SIZE=$(du -sh "${ARCHIVE_FILE}" | cut -f1)
    log "Archive created: ${ARCHIVE_FILE} (${ARCHIVE_SIZE})"

    # Remove uncompressed files
    rm -f "${PG_DUMP_FILE}"
    rm -rf "${MINIO_BACKUP_DIR}"
fi

# --- 5. Weekly/Monthly copies ------------------------------------------------

# Sunday = weekly backup
if [ "${DAY_OF_WEEK}" = "7" ]; then
    log "Creating weekly backup copy..."
    cp "${ARCHIVE_FILE}" "${WEEKLY_DIR}/monteweb_${TIMESTAMP}.tar.gz"
fi

# 1st of month = monthly backup
if [ "${DAY_OF_MONTH}" = "01" ]; then
    log "Creating monthly backup copy..."
    cp "${ARCHIVE_FILE}" "${MONTHLY_DIR}/monteweb_${TIMESTAMP}.tar.gz"
fi

# --- 6. Rotation (cleanup old backups) ---------------------------------------
log "Rotating old backups..."

rotate_backups() {
    local dir="$1"
    local keep="$2"
    local count
    count=$(find "${dir}" -maxdepth 1 -name "monteweb_*.tar.gz" -type f | wc -l)
    if [ "${count}" -gt "${keep}" ]; then
        local to_remove=$((count - keep))
        find "${dir}" -maxdepth 1 -name "monteweb_*.tar.gz" -type f | sort | head -n "${to_remove}" | while read -r f; do
            log "  Removing old backup: $(basename "${f}")"
            rm -f "${f}"
        done
    fi
}

rotate_backups "${DAILY_DIR}" "${RETENTION_DAILY}"
rotate_backups "${WEEKLY_DIR}" "${RETENTION_WEEKLY}"
rotate_backups "${MONTHLY_DIR}" "${RETENTION_MONTHLY}"

# --- 7. Optional S3 remote upload --------------------------------------------
if [ -n "${BACKUP_S3_ENDPOINT}" ] && [ -n "${BACKUP_S3_BUCKET}" ]; then
    log "Uploading to remote S3: ${BACKUP_S3_ENDPOINT}/${BACKUP_S3_BUCKET}..."

    mc alias set remote "${BACKUP_S3_ENDPOINT}" "${BACKUP_S3_ACCESS_KEY}" "${BACKUP_S3_SECRET_KEY}" --quiet 2>/dev/null || true

    if mc cp "${ARCHIVE_FILE}" "remote/${BACKUP_S3_BUCKET}/daily/" --quiet 2>/dev/null; then
        log "Remote upload complete"
    else
        error "Remote S3 upload failed!"
    fi

    # Upload weekly/monthly copies too
    if [ "${DAY_OF_WEEK}" = "7" ]; then
        mc cp "${WEEKLY_DIR}/monteweb_${TIMESTAMP}.tar.gz" "remote/${BACKUP_S3_BUCKET}/weekly/" --quiet 2>/dev/null || true
    fi
    if [ "${DAY_OF_MONTH}" = "01" ]; then
        mc cp "${MONTHLY_DIR}/monteweb_${TIMESTAMP}.tar.gz" "remote/${BACKUP_S3_BUCKET}/monthly/" --quiet 2>/dev/null || true
    fi
fi

# --- Summary -----------------------------------------------------------------
TOTAL_SIZE=$(du -sh "${BACKUP_DIR}" | cut -f1)
DAILY_COUNT=$(find "${DAILY_DIR}" -maxdepth 1 -name "monteweb_*.tar.gz" -type f | wc -l)
WEEKLY_COUNT=$(find "${WEEKLY_DIR}" -maxdepth 1 -name "monteweb_*.tar.gz" -type f | wc -l)
MONTHLY_COUNT=$(find "${MONTHLY_DIR}" -maxdepth 1 -name "monteweb_*.tar.gz" -type f | wc -l)

log "=== Backup complete ==="
log "  Archive: ${ARCHIVE_FILE}"
log "  Total storage: ${TOTAL_SIZE}"
log "  Backups: ${DAILY_COUNT} daily / ${WEEKLY_COUNT} weekly / ${MONTHLY_COUNT} monthly"
log "========================"
