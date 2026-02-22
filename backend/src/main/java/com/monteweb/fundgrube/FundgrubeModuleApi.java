package com.monteweb.fundgrube;

import java.util.Map;
import java.util.UUID;

/**
 * Public API: Facade interface for the fundgrube module.
 */
public interface FundgrubeModuleApi {

    /**
     * DSGVO: Export all fundgrube-related data for a user.
     */
    Map<String, Object> exportUserData(UUID userId);
}
