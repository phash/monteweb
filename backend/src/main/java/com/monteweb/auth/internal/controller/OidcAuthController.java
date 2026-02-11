package com.monteweb.auth.internal.controller;

import com.monteweb.auth.internal.service.JwtService;
import com.monteweb.auth.internal.service.OidcUserService;
import com.monteweb.auth.internal.service.RefreshTokenService;
import com.monteweb.shared.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Handles OIDC/SSO authentication flow.
 *
 * Flow:
 * 1. Frontend calls GET /api/v1/auth/oidc/config to get available providers
 * 2. Frontend redirects to GET /api/v1/auth/oidc/{provider}/login
 * 3. Spring Security handles the OAuth2 flow
 * 4. After callback, frontend exchanges the auth code via POST /api/v1/auth/oidc/token
 */
@RestController
@RequestMapping("/api/v1/auth/oidc")
@ConditionalOnProperty(prefix = "monteweb.oidc", name = "enabled", havingValue = "true")
public class OidcAuthController {

    private final OidcUserService oidcUserService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final ClientRegistrationRepository clientRegistrationRepository;

    @Value("${monteweb.oidc.provider-name:oidc}")
    private String providerName;

    public OidcAuthController(OidcUserService oidcUserService,
                               JwtService jwtService,
                               RefreshTokenService refreshTokenService,
                               ClientRegistrationRepository clientRegistrationRepository) {
        this.oidcUserService = oidcUserService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    /**
     * Returns OIDC configuration for the frontend.
     */
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConfig() {
        var registration = clientRegistrationRepository.findByRegistrationId(providerName);
        if (registration == null) {
            return ResponseEntity.ok(ApiResponse.ok(Map.of("enabled", false)));
        }

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "enabled", true,
                "provider", providerName,
                "authorizationUri", "/oauth2/authorization/" + providerName
        )));
    }

    /**
     * Exchanges OIDC user info for application JWT tokens.
     * Called by the frontend after the OAuth2 redirect flow completes.
     */
    @PostMapping("/token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> exchangeToken(
            @RequestBody OidcTokenRequest request) {
        var user = oidcUserService.resolveUser(
                request.provider(),
                request.subject(),
                request.email(),
                request.firstName(),
                request.lastName()
        );

        String accessToken = jwtService.generateAccessToken(
                user.id(), user.email(), user.role().name());
        String refreshToken = refreshTokenService.createRefreshToken(user.id());

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "userId", user.id(),
                "email", user.email(),
                "role", user.role().name()
        )));
    }

    public record OidcTokenRequest(
            String provider,
            String subject,
            String email,
            String firstName,
            String lastName
    ) {}
}
