package com.monteweb.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Public API: Facade interface for the user module.
 * Other modules interact with users exclusively through this interface.
 */
public interface UserModuleApi {

    Optional<UserInfo> findById(UUID id);

    Optional<UserInfo> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Search users by display name or email. Used by the messaging module for user picker.
     */
    Page<UserInfo> searchUsers(String query, Pageable pageable);

    /**
     * Creates a new user. Used by the auth module during registration.
     *
     * @return the created user's info
     */
    UserInfo createUser(String email, String passwordHash, String firstName, String lastName, String phone, UserRole role);

    /**
     * Returns the hashed password for authentication purposes.
     * Only intended for use by the auth module.
     */
    Optional<String> getPasswordHash(String email);

    /**
     * Updates the last login timestamp.
     */
    void updateLastLogin(UUID userId);

    /**
     * Updates the user's password hash. Used by auth module for password reset.
     */
    void updatePasswordHash(UUID userId, String passwordHash);

    /**
     * Finds a user by OIDC provider and subject. Used for SSO login.
     */
    Optional<UserInfo> findByOidcProviderAndSubject(String provider, String subject);

    /**
     * Creates a user from OIDC claims (no password). Used by the auth module during SSO login.
     */
    UserInfo createOidcUser(String email, String firstName, String lastName,
                            String oidcProvider, String oidcSubject, UserRole role);

    /**
     * Links an existing user to an OIDC provider. Used when a user logs in via SSO
     * and their email matches an existing account.
     */
    void linkOidcProvider(UUID userId, String oidcProvider, String oidcSubject);
}
