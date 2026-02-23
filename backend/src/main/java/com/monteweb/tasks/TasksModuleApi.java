package com.monteweb.tasks;

import java.util.Map;
import java.util.UUID;

/**
 * Public API: Facade interface for the tasks module.
 * Other modules interact with tasks exclusively through this interface.
 */
public interface TasksModuleApi {

    /**
     * DSGVO: Export all task-related data for a user.
     */
    Map<String, Object> exportUserData(UUID userId);
}
