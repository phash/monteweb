FROM alpine:3.20

RUN apk add --no-cache \
    postgresql16-client \
    redis \
    bash \
    curl \
    gzip \
    tzdata \
    && rm -rf /var/cache/apk/*

# Install MinIO Client (mc)
RUN curl -fsSL https://dl.min.io/client/mc/release/linux-amd64/mc -o /usr/local/bin/mc \
    && chmod +x /usr/local/bin/mc

COPY backup.sh /usr/local/bin/backup.sh
COPY restore.sh /usr/local/bin/restore.sh
COPY entrypoint.sh /entrypoint.sh

# Fix Windows line endings and set permissions
RUN sed -i 's/\r$//' /usr/local/bin/backup.sh /usr/local/bin/restore.sh /entrypoint.sh \
    && chmod +x /usr/local/bin/backup.sh /usr/local/bin/restore.sh /entrypoint.sh

# Create backup directory
RUN mkdir -p /backups/daily /backups/weekly /backups/monthly /var/log

ENTRYPOINT ["/entrypoint.sh"]
