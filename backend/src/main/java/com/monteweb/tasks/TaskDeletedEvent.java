package com.monteweb.tasks;

import java.util.UUID;

/**
 * Public API: Published when a task is deleted.
 * The search module listens to this to remove from Solr index.
 */
public record TaskDeletedEvent(
        UUID taskId
) {
}
