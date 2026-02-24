package com.monteweb.files;

import java.util.UUID;

/**
 * Public API: Published when a file is deleted from a room.
 * The search module listens to this to remove from Solr index.
 */
public record FileDeletedEvent(
        UUID fileId
) {
}
