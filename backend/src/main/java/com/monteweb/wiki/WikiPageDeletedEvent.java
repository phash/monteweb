package com.monteweb.wiki;

import java.util.UUID;

/**
 * Public API: Published when a wiki page is deleted.
 * The search module listens to this to remove from Solr index.
 */
public record WikiPageDeletedEvent(
        UUID pageId
) {
}
