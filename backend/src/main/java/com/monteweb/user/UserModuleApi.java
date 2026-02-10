package com.monteweb.user;

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
}
