package com.monteweb.auth;

import com.monteweb.user.UserInfo;

/**
 * Public API: Facade interface for the auth module.
 * Other modules interact with auth exclusively through this interface.
 */
public interface AuthModuleApi {

    /**
     * Generates a new access+refresh token pair for a user.
     * Used when the user's role changes (e.g., role switching) and new tokens are needed.
     */
    TokenResponse generateTokensForUser(UserInfo user);
}
