#!/bin/bash
set -e

# Write cron schedule from environment variable
CRON="${BACKUP_CRON:-0 2 * * *}"
echo "${CRON} /usr/local/bin/backup.sh >> /var/log/backup.log 2>&1" > /etc/crontabs/root

echo "=== MonteWeb Backup Service ==="
echo "Schedule: ${CRON}"
echo "Retention: ${BACKUP_RETENTION_DAILY:-7} daily / ${BACKUP_RETENTION_WEEKLY:-4} weekly / ${BACKUP_RETENTION_MONTHLY:-3} monthly"
echo "Remote S3: ${BACKUP_S3_ENDPOINT:-disabled}"
echo "==============================="

# Run initial backup if requested
if [ "${BACKUP_RUN_ON_START:-false}" = "true" ]; then
    echo "Running initial backup..."
    /usr/local/bin/backup.sh
fi

# Start crond in foreground
exec crond -f -l 2
