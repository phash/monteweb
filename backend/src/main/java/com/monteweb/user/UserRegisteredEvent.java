package com.monteweb.user;

import java.util.UUID;

/**
 * Public API: Published when a new user registers.
 * Other modules can listen to this event to perform initialization.
 */
public record UserRegisteredEvent(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        UserRole role
) {
}
