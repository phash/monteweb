package com.monteweb.search.internal.service;

import com.monteweb.calendar.CalendarModuleApi;
import com.monteweb.calendar.EventCreatedEvent;
import com.monteweb.calendar.EventDeletedEvent;
import com.monteweb.feed.FeedModuleApi;
import com.monteweb.feed.FeedPostCreatedEvent;
import com.monteweb.files.FileDeletedEvent;
import com.monteweb.files.FileUploadedEvent;
import com.monteweb.files.FilesModuleApi;
import com.monteweb.room.RoomCreatedEvent;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.tasks.TaskDeletedEvent;
import com.monteweb.tasks.TaskSavedEvent;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRegisteredEvent;
import com.monteweb.wiki.WikiPageDeletedEvent;
import com.monteweb.wiki.WikiPageSavedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

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
    private final UserModuleApi userModuleApi;
    private final RoomModuleApi roomModuleApi;

    @Autowired(required = false)
    private FeedModuleApi feedModuleApi;

    @Autowired(required = false)
    private FilesModuleApi filesModuleApi;

    @Autowired(required = false)
    private CalendarModuleApi calendarModuleApi;

    public SolrIndexingListener(SolrIndexingService indexingService,
                                UserModuleApi userModuleApi,
                                RoomModuleApi roomModuleApi) {
        this.indexingService = indexingService;
        this.userModuleApi = userModuleApi;
        this.roomModuleApi = roomModuleApi;
    }

    @ApplicationModuleListener
    public void onFeedPostCreated(FeedPostCreatedEvent event) {
        log.debug("Indexing feed post: {}", event.postId());
        if (feedModuleApi == null) return;
        feedModuleApi.findPostById(event.postId())
                .ifPresent(indexingService::indexFeedPost);
    }

    @ApplicationModuleListener
    public void onUserRegistered(UserRegisteredEvent event) {
        log.debug("Indexing user: {}", event.userId());
        userModuleApi.findById(event.userId())
                .ifPresent(indexingService::indexUser);
    }

    @ApplicationModuleListener
    public void onRoomCreated(RoomCreatedEvent event) {
        log.debug("Indexing room: {}", event.roomId());
        roomModuleApi.findById(event.roomId())
                .ifPresent(indexingService::indexRoom);
    }

    @ApplicationModuleListener
    public void onEventCreated(EventCreatedEvent event) {
        log.debug("Indexing calendar event: {}", event.eventId());
        if (calendarModuleApi == null) return;
        calendarModuleApi.findById(event.eventId())
                .ifPresent(indexingService::indexEvent);
    }

    @ApplicationModuleListener
    public void onEventDeleted(EventDeletedEvent event) {
        log.debug("Removing calendar event from index: {}", event.eventId());
        indexingService.deleteDocument("EVENT", event.eventId());
    }

    @ApplicationModuleListener
    public void onFileUploaded(FileUploadedEvent event) {
        log.debug("Indexing uploaded file: {}", event.fileId());

        // Tika full-text extraction (indexFileWithContent) requires streaming the
        // file's bytes to Solr's /update/extract handler. The files module currently
        // only exposes getStoragePath(fileId) via FilesModuleApi, not an InputStream,
        // and the search module has no MinIO client of its own — so we cannot obtain
        // the content here. We index metadata only until FilesModuleApi exposes a
        // content-stream accessor (see FLAG in the audit changelog).
        if (filesModuleApi != null && event.contentType() != null && isExtractable(event.contentType())) {
            log.debug("File {} is an extractable type ({}); indexing metadata only "
                    + "(Tika extraction needs a content stream from the files module)",
                    event.fileId(), event.contentType());
        }

        indexingService.indexFile(event.fileId(), event.roomId(),
                event.originalName(), event.contentType(), event.fileSize());
    }

    @ApplicationModuleListener
    public void onFileDeleted(FileDeletedEvent event) {
        log.debug("Removing file from index: {}", event.fileId());
        indexingService.deleteDocument("FILE", event.fileId());
    }

    @ApplicationModuleListener
    public void onWikiPageSaved(WikiPageSavedEvent event) {
        log.debug("Indexing wiki page: {}", event.pageId());
        indexingService.indexWikiPage(event.pageId(), event.roomId(),
                event.title(), event.content(), event.slug());
    }

    @ApplicationModuleListener
    public void onWikiPageDeleted(WikiPageDeletedEvent event) {
        log.debug("Removing wiki page from index: {}", event.pageId());
        indexingService.deleteDocument("WIKI", event.pageId());
    }

    @ApplicationModuleListener
    public void onTaskSaved(TaskSavedEvent event) {
        log.debug("Indexing task: {}", event.taskId());
        indexingService.indexTask(event.taskId(), event.roomId(),
                event.title(), event.description(), event.assigneeName());
    }

    @ApplicationModuleListener
    public void onTaskDeleted(TaskDeletedEvent event) {
        log.debug("Removing task from index: {}", event.taskId());
        indexingService.deleteDocument("TASK", event.taskId());
    }

    private boolean isExtractable(String contentType) {
        return EXTRACTABLE_TYPES.contains(contentType);
    }
}
