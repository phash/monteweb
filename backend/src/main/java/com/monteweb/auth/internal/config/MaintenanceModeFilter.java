package com.monteweb.auth.internal.config;

import com.monteweb.admin.AdminModuleApi;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class MaintenanceModeFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_PREFIXES = Set.of(
            "/api/v1/auth/",
            "/api/v1/config",
            "/api/v1/admin/",
            "/actuator/health",
            "/ws/"
    );

    private final AdminModuleApi adminModuleApi;

    public MaintenanceModeFilter(@Autowired(required = false) AdminModuleApi adminModuleApi) {
        this.adminModuleApi = adminModuleApi;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // If admin module is not available, skip
        if (adminModuleApi == null || !adminModuleApi.isMaintenanceEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // Allow whitelisted paths
        for (String prefix : ALLOWED_PREFIXES) {
            if (path.startsWith(prefix) || path.equals(prefix)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        // Allow OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Allow SUPERADMIN users
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"))) {
            filterChain.doFilter(request, response);
            return;
        }

        // Block with 503
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        var config = adminModuleApi.getTenantConfig();
        String message = config.maintenanceMessage() != null
                ? config.maintenanceMessage()
                : "System is under maintenance";

        // Write JSON manually to avoid ObjectMapper dependency during filter init
        String json = "{\"maintenance\":true,\"message\":\"" + message.replace("\"", "\\\"") + "\"}";
        response.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
    }
}
