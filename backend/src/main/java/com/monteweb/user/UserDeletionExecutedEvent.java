package com.monteweb.user;

import java.util.UUID;

/**
 * Public API: Published when a user's account is actually deleted/anonymized.
 * Modules must listen to this event to clean up their own data for the user.
 */
public record UserDeletionExecutedEvent(UUID userId) {
}
