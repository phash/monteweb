package com.monteweb.wiki;

import java.util.UUID;

/**
 * Public API: Published when a wiki page is created or updated.
 * The search module listens to this for Solr indexing.
 */
public record WikiPageSavedEvent(
        UUID pageId,
        UUID roomId,
        String title,
        String content,
        String slug
) {
}
