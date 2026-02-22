package com.monteweb.auth.internal.config;

import com.monteweb.auth.internal.service.OidcAuthCodeStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles successful OAuth2/OIDC authentication.
 * Stores verified user claims in Redis with a one-time code,
 * then redirects the user to the frontend with the code.
 */
@Component
@ConditionalOnProperty(prefix = "monteweb.oidc", name = "enabled", havingValue = "true")
public class OidcAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OidcAuthenticationSuccessHandler.class);

    private final OidcAuthCodeStore codeStore;

    @Value("${monteweb.cors.allowed-origins:http://localhost:5173}")
    private String frontendUrl;

    @Value("${monteweb.oidc.provider-name:oidc}")
    private String providerName;

    public OidcAuthenticationSuccessHandler(OidcAuthCodeStore codeStore) {
        this.codeStore = codeStore;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            log.error("OIDC authentication success but principal is not OidcUser");
            response.sendRedirect(frontendUrl().concat("/login?error=oidc_failed"));
            return;
        }

        String subject = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String firstName = oidcUser.getGivenName();
        String lastName = oidcUser.getFamilyName();

        if (email == null || subject == null) {
            log.error("OIDC user missing required claims: email={}, subject={}", email, subject);
            response.sendRedirect(frontendUrl().concat("/login?error=oidc_missing_claims"));
            return;
        }

        String code = codeStore.storeVerifiedClaims(providerName, subject, email, firstName, lastName);
        log.info("OIDC authentication successful for {}, redirecting with auth code", email);
        response.sendRedirect(frontendUrl().concat("/login?oidc_code=" + code));
    }

    private String frontendUrl() {
        // Take the first origin if multiple are configured
        String url = frontendUrl.split(",")[0].trim();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
