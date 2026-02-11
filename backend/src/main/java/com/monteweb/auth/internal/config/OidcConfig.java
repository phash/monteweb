package com.monteweb.auth.internal.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.beans.factory.annotation.Value;

/**
 * Conditional OIDC configuration. Only active when monteweb.oidc.enabled=true.
 * Creates the ClientRegistrationRepository manually to avoid auto-config issues
 * when OIDC is disabled.
 */
@Configuration
@ConditionalOnProperty(prefix = "monteweb.oidc", name = "enabled", havingValue = "true")
public class OidcConfig {

    @Value("${monteweb.oidc.client-id}")
    private String clientId;

    @Value("${monteweb.oidc.client-secret}")
    private String clientSecret;

    @Value("${monteweb.oidc.issuer-uri}")
    private String issuerUri;

    @Value("${monteweb.oidc.provider-name:oidc}")
    private String providerName;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration registration = ClientRegistration.withRegistrationId(providerName)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .scope("openid", "profile", "email")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri(issuerUri + "/protocol/openid-connect/auth")
                .tokenUri(issuerUri + "/protocol/openid-connect/token")
                .userInfoUri(issuerUri + "/protocol/openid-connect/userinfo")
                .jwkSetUri(issuerUri + "/protocol/openid-connect/certs")
                .userNameAttributeName("sub")
                .build();

        return new InMemoryClientRegistrationRepository(registration);
    }
}
