package com.monteweb.shared.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<UUID> getCurrentUserId() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .filter(name -> !"anonymousUser".equals(name))
                .map(UUID::fromString);
    }

    public static UUID requireCurrentUserId() {
        return getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("No authenticated user"));
    }
}
