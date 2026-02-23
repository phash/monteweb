package com.monteweb.search;

import java.time.Instant;
import java.util.UUID;

/**
 * Public API: A single search result from the global search.
 */
public record SearchResult(
        UUID id,
        String type,       // "POST", "USER", "ROOM", "EVENT"
        String title,
        String subtitle,
        String snippet,    // matched text snippet
        String url,        // frontend route to navigate to
        Instant timestamp
) {
}
