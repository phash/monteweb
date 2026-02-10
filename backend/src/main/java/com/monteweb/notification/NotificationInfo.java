package com.monteweb.notification;

import java.time.Instant;
import java.util.UUID;

/**
 * Public API: Read-only notification information for cross-module use.
 */
public record NotificationInfo(
        UUID id,
        UUID userId,
        NotificationType type,
        String title,
        String message,
        String link,
        String referenceType,
        UUID referenceId,
        boolean read,
        Instant createdAt
) {
}
