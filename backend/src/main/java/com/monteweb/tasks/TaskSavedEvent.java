package com.monteweb.tasks;

import java.util.UUID;

/**
 * Public API: Published when a task is created or updated.
 * The search module listens to this for Solr indexing.
 */
public record TaskSavedEvent(
        UUID taskId,
        UUID roomId,
        String title,
        String description,
        String assigneeName
) {
}
