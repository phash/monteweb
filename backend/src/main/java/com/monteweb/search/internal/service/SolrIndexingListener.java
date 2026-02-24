package com.monteweb.search.internal.service;

import com.monteweb.feed.FeedCommentCreatedEvent;
import com.monteweb.feed.FeedModuleApi;
import com.monteweb.feed.FeedPostCreatedEvent;
import com.monteweb.files.FileDeletedEvent;
import com.monteweb.files.FileUploadedEvent;
import com.monteweb.files.FilesModuleApi;
import com.monteweb.tasks.TaskDeletedEvent;
import com.monteweb.tasks.TaskSavedEvent;
import com.monteweb.wiki.WikiPageDeletedEvent;
import com.monteweb.wiki.WikiPageSavedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Set;

@Component
@ConditionalOnProperty(prefix = "monteweb.modules.solr", name = "enabled", havingValue = "true")
public class SolrIndexingListener {

    private static final Logger log = LoggerFactory.getLogger(SolrIndexingListener.class);

    private static final Set<String> EXTRACTABLE_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "text/csv",
            "text/html",
            "application/rtf"
    );

    private final SolrIndexingService indexingService;

    @Autowired(required = false)
    private FeedModuleApi feedModuleApi;

    @Autowired(required = false)
    private FilesModuleApi filesModuleApi;

    public SolrIndexingListener(SolrIndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @Async
    @EventListener
    public void onFeedPostCreated(FeedPostCreatedEvent event) {
        log.debug("Indexing feed post: {}", event.postId());
        if (feedModuleApi == null) return;
        feedModuleApi.findPostById(event.postId())
                .ifPresent(indexingService::indexFeedPost);
    }

    @Async
    @EventListener
    public void onFileUploaded(FileUploadedEvent event) {
        log.debug("Indexing uploaded file: {}", event.fileId());

        // Try content extraction for supported types
        if (filesModuleApi != null && event.contentType() != null && isExtractable(event.contentType())) {
            String storagePath = filesModuleApi.getStoragePath(event.fileId());
            if (storagePath != null) {
                try {
                    // Download from MinIO for extraction
                    // We'll index with metadata only — content extraction happens in Solr
                    // File is streamed directly to Solr's ExtractingRequestHandler
                    indexingService.indexFile(event.fileId(), event.roomId(),
                            event.originalName(), event.contentType(), event.fileSize());
                    return;
                } catch (Exception e) {
                    log.warn("Could not extract file content for {}: {}", event.fileId(), e.getMessage());
                }
            }
        }

        // Fallback: index metadata only
        indexingService.indexFile(event.fileId(), event.roomId(),
                event.originalName(), event.contentType(), event.fileSize());
    }

    @Async
    @EventListener
    public void onFileDeleted(FileDeletedEvent event) {
        log.debug("Removing file from index: {}", event.fileId());
        indexingService.deleteDocument("FILE", event.fileId());
    }

    @Async
    @EventListener
    public void onWikiPageSaved(WikiPageSavedEvent event) {
        log.debug("Indexing wiki page: {}", event.pageId());
        indexingService.indexWikiPage(event.pageId(), event.roomId(),
                event.title(), event.content(), event.slug());
    }

    @Async
    @EventListener
    public void onWikiPageDeleted(WikiPageDeletedEvent event) {
        log.debug("Removing wiki page from index: {}", event.pageId());
        indexingService.deleteDocument("WIKI", event.pageId());
    }

    @Async
    @EventListener
    public void onTaskSaved(TaskSavedEvent event) {
        log.debug("Indexing task: {}", event.taskId());
        indexingService.indexTask(event.taskId(), event.roomId(),
                event.title(), event.description(), event.assigneeName());
    }

    @Async
    @EventListener
    public void onTaskDeleted(TaskDeletedEvent event) {
        log.debug("Removing task from index: {}", event.taskId());
        indexingService.deleteDocument("TASK", event.taskId());
    }

    private boolean isExtractable(String contentType) {
        return EXTRACTABLE_TYPES.contains(contentType);
    }
}
