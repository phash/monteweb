package com.monteweb.auth.internal.controller;

import com.monteweb.shared.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Always-available OIDC config endpoint.
 * Returns whether OIDC is enabled so the frontend can show/hide the SSO button.
 * This controller is NOT conditional — it exists even when OIDC is disabled.
 */
@RestController
@RequestMapping("/api/v1/auth/oidc")
public class OidcConfigController {

    private final ClientRegistrationRepository clientRegistrationRepository;

    @Value("${monteweb.oidc.provider-name:oidc}")
    private String providerName;

    public OidcConfigController(
            @Autowired(required = false) ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConfig() {
        if (clientRegistrationRepository == null) {
            return ResponseEntity.ok(ApiResponse.ok(Map.of("enabled", false)));
        }

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
}
