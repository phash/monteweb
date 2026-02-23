package com.monteweb.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
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

    /**
     * Finds users that have a specific special role (e.g. "SECTION_ADMIN:uuid").
     */
    List<UserInfo> findBySpecialRoleContaining(String role);

    /**
     * Finds multiple users by their IDs. Returns only the ones that exist.
     */
    List<UserInfo> findByIds(List<UUID> ids);

    /**
     * Switches the active role for a user. The new role must be in the user's assignedRoles.
     * SUPERADMIN and STUDENT cannot switch roles.
     */
    UserInfo switchActiveRole(UUID userId, UserRole newRole);

    /**
     * Activates or deactivates a user. Used by auth module after registration.
     */
    UserInfo setActive(UUID userId, boolean active);

    /**
     * Updates a user's profile information. Used by auth module for LDAP attribute sync.
     */
    UserInfo updateProfile(UUID userId, String firstName, String lastName, String phone);

    // --- TOTP / 2FA ---

    /**
     * Returns whether the user has TOTP enabled.
     */
    boolean isTotpEnabled(UUID userId);

    /**
     * Returns the stored TOTP secret for the user, or empty if not set.
     */
    Optional<String> getTotpSecret(UUID userId);

    /**
     * Stores the TOTP secret for a user (setup phase, before confirmation).
     */
    void setTotpSecret(UUID userId, String secret);

    /**
     * Enables TOTP for a user and stores recovery codes.
     */
    void enableTotp(UUID userId, String[] recoveryCodes);

    /**
     * Disables TOTP for a user, clearing secret and recovery codes.
     */
    void disableTotp(UUID userId);

    /**
     * Returns the stored recovery codes for the user.
     */
    String[] getTotpRecoveryCodes(UUID userId);

    /**
     * Replaces the stored recovery codes (e.g. after one is consumed).
     */
    void setTotpRecoveryCodes(UUID userId, String[] codes);
}
