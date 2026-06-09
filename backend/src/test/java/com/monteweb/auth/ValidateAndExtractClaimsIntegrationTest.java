package com.monteweb.auth;

import com.monteweb.TestContainerConfig;
import com.monteweb.auth.internal.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Security tests for {@link AuthModuleApi#validateAndExtractClaims(String)} — the single
 * entry point used by the WebSocket/STOMP authentication interceptor.
 *
 * Special-purpose tokens that carry a "type" claim (2FA temp tokens issued BEFORE the second
 * factor is verified, and freely-mintable image tokens) must NOT authenticate a session.
 * Only full access tokens (no "type" claim) may. See auth security review finding [1].
 */
@SpringBootTest
@Import(TestContainerConfig.class)
class ValidateAndExtractClaimsIntegrationTest {

    @Autowired
    private AuthModuleApi authModuleApi;

    @Autowired
    private JwtService jwtService;

    @Test
    void validateAndExtractClaims_rejects2faTempToken() {
        UUID userId = UUID.randomUUID();
        // A 2FA temp token is issued before the second factor is verified — it must never
        // authenticate a realtime session, otherwise mandatory 2FA is bypassed.
        String tempToken = jwtService.generateTempToken(userId, "user@example.com", "TEACHER");

        assertThat(authModuleApi.validateAndExtractClaims(tempToken)).isEmpty();
    }

    @Test
    void validateAndExtractClaims_rejectsImageToken() {
        UUID userId = UUID.randomUUID();
        // Image tokens are scoped to image endpoints only — they carry no role and must not
        // authenticate a session they were never issued for.
        String imageToken = authModuleApi.generateImageToken(userId);

        assertThat(authModuleApi.validateAndExtractClaims(imageToken)).isEmpty();
    }

    @Test
    void validateAndExtractClaims_acceptsRegularAccessToken() {
        UUID userId = UUID.randomUUID();
        // A normal access token (no "type" claim) must still authenticate and expose the role.
        String accessToken = jwtService.generateAccessToken(userId, "user@example.com", "PARENT");

        var claims = authModuleApi.validateAndExtractClaims(accessToken);
        assertThat(claims).isPresent();
        assertThat(claims.get().userId()).isEqualTo(userId.toString());
        assertThat(claims.get().role()).isEqualTo("PARENT");
    }

    @Test
    void validateAndExtractClaims_rejectsNullAndGarbage() {
        assertThat(authModuleApi.validateAndExtractClaims(null)).isEmpty();
        assertThat(authModuleApi.validateAndExtractClaims("not-a-jwt")).isEmpty();
    }
}
