package com.monteweb.auth.internal.config;

import com.monteweb.auth.internal.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null) {
            if (isImageEndpoint(request)) {
                // Image endpoints: accept short-lived image tokens (preferred) or regular JWT
                authenticateImageToken(request, token);
            } else if (jwtService.validateToken(token)) {
                authenticateWithJwt(request, token);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateImageToken(HttpServletRequest request, String token) {
        // First try as image token, then fall back to regular JWT for backwards compatibility
        var imageUserId = jwtService.validateImageToken(token);
        if (imageUserId.isPresent()) {
            var authentication = new UsernamePasswordAuthenticationToken(
                    imageUserId.get(), null, new ArrayList<>()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else if (jwtService.validateToken(token)) {
            // Backwards compatibility: accept regular JWT (will be removed in future)
            authenticateWithJwt(request, token);
        }
    }

    private void authenticateWithJwt(HttpServletRequest request, String token) {
        var claims = jwtService.extractClaims(token);
        String userId = claims.getSubject();
        String role = claims.get("role", String.class);

        // Reject regular JWT tokens that have "type" claim (e.g., image tokens used for API calls)
        if (claims.get("type", String.class) != null) {
            return;
        }

        var authorities = new ArrayList<SimpleGrantedAuthority>();
        if (role != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }

        var authentication = new UsernamePasswordAuthenticationToken(
                userId, null, authorities
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String extractToken(HttpServletRequest request) {
        // 1. Authorization header (preferred for API clients)
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        // 2. httpOnly access_token cookie (set by login/refresh endpoints)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }
        // 3. Query parameter for image endpoints (img tags can't send headers)
        if (isImageEndpoint(request)) {
            String queryToken = request.getParameter("token");
            if (StringUtils.hasText(queryToken)) {
                return queryToken;
            }
        }
        return null;
    }

    private boolean isImageEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && (
                path.startsWith("/api/v1/fotobox/images/") ||
                path.startsWith("/api/v1/fundgrube/images/") ||
                path.startsWith("/api/v1/messages/images/")
        );
    }
}
