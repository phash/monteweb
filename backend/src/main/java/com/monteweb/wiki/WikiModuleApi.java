package com.monteweb.wiki;

import java.util.Map;
import java.util.UUID;

/**
 * Public API: Facade interface for the wiki module.
 * Other modules interact with wiki exclusively through this interface.
 */
public interface WikiModuleApi {

    /**
     * DSGVO: Export all wiki-related data for a user.
     */
    Map<String, Object> exportUserData(UUID userId);
}
