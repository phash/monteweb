package com.monteweb.user;

import java.util.UUID;

/**
 * Public API: Published when a user cancels their pending account deletion.
 */
public record UserDeletionCancelledEvent(UUID userId) {
}
