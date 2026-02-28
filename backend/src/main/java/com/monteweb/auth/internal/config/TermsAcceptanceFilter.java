package com.monteweb.auth.internal.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monteweb.admin.AdminModuleApi;
import com.monteweb.user.UserModuleApi;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class TermsAcceptanceFilter extends OncePerRequestFilter {

    private final UserModuleApi userModuleApi;
    private final AdminModuleApi adminModuleApi;
    private final ObjectMapper objectMapper;

    public TermsAcceptanceFilter(UserModuleApi userModuleApi,
                                  AdminModuleApi adminModuleApi,
                                  ObjectMapper objectMapper) {
        this.userModuleApi = userModuleApi;
        this.adminModuleApi = adminModuleApi;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        String method = request.getMethod();

        // Whitelist: no terms check needed
        if ("OPTIONS".equals(method)
                || path.startsWith("/api/v1/auth/")
                || path.startsWith("/api/v1/privacy/")
                || path.equals("/api/v1/config")
                || path.equals("/api/v1/error-reports")
                || path.startsWith("/actuator/")
                || path.startsWith("/ws/")
                || path.startsWith("/wopi/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Only check authenticated users
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check if terms version is configured
        String termsVersion = adminModuleApi.getTenantConfig().termsVersion();
        if (termsVersion == null || termsVersion.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check acceptance
        String principal = authentication.getName();
        try {
            UUID userUuid = UUID.fromString(principal);
            boolean accepted = userModuleApi.hasAcceptedTerms(userUuid, termsVersion);
            if (!accepted) {
                response.setStatus(451);
                response.setContentType("application/json");
                objectMapper.writeValue(response.getWriter(),
                    Map.of("termsRequired", true, "termsVersion", termsVersion));
                return;
            }
        } catch (IllegalArgumentException e) {
            // Non-UUID principal (e.g. OIDC subject), skip check
        }

        filterChain.doFilter(request, response);
    }
}
