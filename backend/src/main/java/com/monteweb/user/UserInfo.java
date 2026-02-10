package com.monteweb.user;

import java.util.Set;
import java.util.UUID;

/**
 * Public API: Read-only user information for cross-module use.
 */
public record UserInfo(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String displayName,
        String phone,
        String avatarUrl,
        UserRole role,
        Set<String> specialRoles,
        boolean active
) {
}
