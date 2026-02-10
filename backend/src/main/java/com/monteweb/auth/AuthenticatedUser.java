package com.monteweb.auth;

import java.util.Set;
import java.util.UUID;

/**
 * Read-only representation of the currently authenticated user.
 * This is the public API of the auth module - other modules should use this
 * to get information about the current user.
 */
public record AuthenticatedUser(
        UUID id,
        String email,
        String role,
        Set<String> specialRoles
) {
}
