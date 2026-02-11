package com.monteweb.auth.internal.service;

import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Handles OIDC user resolution: find existing user by OIDC subject,
 * link by email, or create a new user from OIDC claims.
 */
@Service
@ConditionalOnProperty(prefix = "monteweb.oidc", name = "enabled", havingValue = "true")
public class OidcUserService {

    private static final Logger log = LoggerFactory.getLogger(OidcUserService.class);

    private final UserModuleApi userModuleApi;

    public OidcUserService(UserModuleApi userModuleApi) {
        this.userModuleApi = userModuleApi;
    }

    /**
     * Resolves or creates a user from OIDC claims.
     * 1. Try to find by OIDC provider+subject
     * 2. Try to find by email and link the OIDC identity
     * 3. Create a new user
     */
    public UserInfo resolveUser(String provider, String subject, String email,
                                String firstName, String lastName) {
        // 1. Already linked OIDC user?
        Optional<UserInfo> byOidc = userModuleApi.findByOidcProviderAndSubject(provider, subject);
        if (byOidc.isPresent()) {
            log.debug("OIDC user found by provider/subject: {}", byOidc.get().email());
            userModuleApi.updateLastLogin(byOidc.get().id());
            return byOidc.get();
        }

        // 2. Email-based linking: existing user with same email?
        Optional<UserInfo> byEmail = userModuleApi.findByEmail(email);
        if (byEmail.isPresent()) {
            log.info("Linking OIDC identity to existing user: {}", email);
            userModuleApi.linkOidcProvider(byEmail.get().id(), provider, subject);
            userModuleApi.updateLastLogin(byEmail.get().id());
            return byEmail.get();
        }

        // 3. Create new user from OIDC claims
        log.info("Creating new user from OIDC: {} ({})", email, provider);
        return userModuleApi.createOidcUser(
                email,
                firstName != null ? firstName : "User",
                lastName != null ? lastName : "",
                provider,
                subject,
                UserRole.PARENT
        );
    }
}
