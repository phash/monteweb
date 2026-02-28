package com.monteweb.search.internal;

import com.monteweb.search.internal.service.SolrIndexingService;
import com.monteweb.user.UserDeletionExecutedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DSGVO: Cleans up Solr index entries when a user account is deleted.
 * Removes the user document from the Solr index.
 */
@Component
@ConditionalOnProperty(prefix = "monteweb.modules.solr", name = "enabled", havingValue = "true")
public class SearchDeletionListener {

    private static final Logger log = LoggerFactory.getLogger(SearchDeletionListener.class);

    private final SolrIndexingService searchService;

    public SearchDeletionListener(SolrIndexingService searchService) {
        this.searchService = searchService;
    }

    @Async
    @EventListener
    @Transactional
    public void onUserDeletion(UserDeletionExecutedEvent event) {
        log.info("Cleaning up Solr index for deleted user {}", event.userId());
        searchService.deleteUserDocuments(event.userId());
    }
}
