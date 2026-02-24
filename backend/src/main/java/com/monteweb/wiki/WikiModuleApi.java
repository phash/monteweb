package com.monteweb.wiki;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Public API: Facade interface for the wiki module.
 * Other modules interact with wiki exclusively through this interface.
 */
public interface WikiModuleApi {

    /**
     * Returns all wiki pages for Solr re-indexing.
     * Each entry: {id, roomId, title, content, slug}
     */
    List<Map<String, Object>> findAllPagesForIndexing();

    /**
     * DSGVO: Export all wiki-related data for a user.
     */
    Map<String, Object> exportUserData(UUID userId);
}
