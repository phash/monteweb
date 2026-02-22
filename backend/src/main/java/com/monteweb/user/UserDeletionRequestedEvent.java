package com.monteweb.user;

import java.time.Instant;
import java.util.UUID;

/**
 * Public API: Published when a user requests account deletion.
 * The deletion is scheduled for a future date (grace period).
 */
public record UserDeletionRequestedEvent(UUID userId, Instant scheduledAt) {
}
