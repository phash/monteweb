package com.monteweb.tasks;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Public API: Facade interface for the tasks module.
 * Other modules interact with tasks exclusively through this interface.
 */
public interface TasksModuleApi {

    /**
     * Returns all tasks for Solr re-indexing.
     * Each entry: {id, roomId, title, description}
     */
    List<Map<String, Object>> findAllTasksForIndexing();

    /**
     * DSGVO: Export all task-related data for a user.
     */
    Map<String, Object> exportUserData(UUID userId);
}
