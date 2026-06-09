package com.monteweb.shared.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

public final class SecurityUtils {

    /**
     * Request attribute set by the JWT authentication filter when the current
     * request is performed under an active impersonation session. Holds the
     * UUID (as String) of the administrator who started impersonation.
     */
    public static final String IMPERSONATED_BY_ATTRIBUTE = "impersonatedBy";

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

    /**
     * Returns the UUID of the administrator currently impersonating another user,
     * if any. Reads the {@link #IMPERSONATED_BY_ATTRIBUTE} request attribute that is
     * populated by the JWT authentication filter from a server-issued, signed token.
     *
     * <p>Use this for audit-log attribution: actions performed during impersonation
     * must be attributed to the impersonating admin, not to the target (impersonated)
     * user returned by {@link #requireCurrentUserId()}.
     *
     * @return the impersonating admin's id, or empty when not impersonating / no request
     */
    public static Optional<UUID> getImpersonatorId() {
        return currentRequest()
                .map(req -> req.getAttribute(IMPERSONATED_BY_ATTRIBUTE))
                .filter(value -> value instanceof String)
                .map(Object::toString)
                .filter(value -> !value.isBlank())
                .map(SecurityUtils::parseUuidOrNull)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    /**
     * Returns the effective actor for audit purposes: the impersonating admin when an
     * impersonation session is active, otherwise the authenticated current user.
     *
     * <p>This guarantees that actions taken while impersonating are attributed to the
     * admin who initiated the session rather than to the impersonated target user.
     */
    public static UUID requireAuditActorId() {
        return getImpersonatorId().orElseGet(SecurityUtils::requireCurrentUserId);
    }

    /**
     * Extracts the real client IP address from the current HTTP request, honouring the
     * reverse-proxy forwarding headers used in front of the application (X-Forwarded-For,
     * then X-Real-IP), and finally falling back to the socket remote address.
     *
     * <p>This MUST be used for security audit trails (e.g. terms acceptance) instead of
     * trusting any client-supplied request-body field, which is spoofable.
     *
     * @return the resolved client IP, or {@code null} when no request context is available
     */
    public static String getClientIpAddress() {
        return currentRequest().map(SecurityUtils::extractClientIp).orElse(null);
    }

    private static String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // X-Forwarded-For may contain a comma-separated list; the first entry is the originating client.
            int comma = forwardedFor.indexOf(',');
            String first = (comma >= 0 ? forwardedFor.substring(0, comma) : forwardedFor).trim();
            if (!first.isBlank()) {
                return first;
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private static Optional<HttpServletRequest> currentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return Optional.of(servletAttributes.getRequest());
        }
        return Optional.empty();
    }

    private static Optional<UUID> parseUuidOrNull(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
