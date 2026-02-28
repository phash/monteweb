package com.monteweb.search.internal.service;

import com.monteweb.calendar.CalendarModuleApi;
import com.monteweb.calendar.EventInfo;
import com.monteweb.feed.FeedModuleApi;
import com.monteweb.feed.FeedPostInfo;
import com.monteweb.files.FileInfo;
import com.monteweb.files.FilesModuleApi;
import com.monteweb.room.RoomInfo;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.tasks.TasksModuleApi;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import com.monteweb.wiki.WikiModuleApi;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "monteweb.modules.solr", name = "enabled", havingValue = "true")
public class SolrIndexingService {

    private static final Logger log = LoggerFactory.getLogger(SolrIndexingService.class);

    private final SolrClient solrClient;
    private final UserModuleApi userModuleApi;
    private final RoomModuleApi roomModuleApi;

    @Autowired(required = false)
    private FeedModuleApi feedModuleApi;
    @Autowired(required = false)
    private CalendarModuleApi calendarModuleApi;
    @Autowired(required = false)
    private FilesModuleApi filesModuleApi;
    @Autowired(required = false)
    private WikiModuleApi wikiModuleApi;
    @Autowired(required = false)
    private TasksModuleApi tasksModuleApi;

    public SolrIndexingService(SolrClient solrClient,
                               UserModuleApi userModuleApi,
                               RoomModuleApi roomModuleApi) {
        this.solrClient = solrClient;
        this.userModuleApi = userModuleApi;
        this.roomModuleApi = roomModuleApi;
    }

    // ---- Single document indexing ----

    public void indexFeedPost(FeedPostInfo post) {
        try {
            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "POST:" + post.id());
            doc.addField("doc_type", "POST");
            doc.addField("entity_id", post.id().toString());
            doc.addField("title", post.title());
            doc.addField("content", post.content());
            doc.addField("author_name", post.authorName());
            doc.addField("source_type", post.sourceType() != null ? post.sourceType().name() : null);
            doc.addField("source_id", post.sourceId() != null ? post.sourceId().toString() : null);
            doc.addField("url", "/feed?post=" + post.id());
            doc.addField("created_at", toDate(post.publishedAt() != null ? post.publishedAt() : post.createdAt()));
            solrClient.add(doc);
            solrClient.commit();
        } catch (SolrServerException | IOException e) {
            log.error("Failed to index feed post {}: {}", post.id(), e.getMessage());
        }
    }

    public void indexWikiPage(UUID pageId, UUID roomId, String title, String content, String slug) {
        try {
            String roomName = roomModuleApi.findById(roomId).map(RoomInfo::name).orElse(null);
            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "WIKI:" + pageId);
            doc.addField("doc_type", "WIKI");
            doc.addField("entity_id", pageId.toString());
            doc.addField("title", title);
            doc.addField("content", content);
            doc.addField("room_id", roomId.toString());
            doc.addField("room_name", roomName);
            doc.addField("url", "/rooms/" + roomId + "/wiki/" + slug);
            doc.addField("updated_at", toDate(Instant.now()));
            solrClient.add(doc);
            solrClient.commit();
        } catch (SolrServerException | IOException e) {
            log.error("Failed to index wiki page {}: {}", pageId, e.getMessage());
        }
    }

    public void indexTask(UUID taskId, UUID roomId, String title, String description, String assigneeName) {
        try {
            String roomName = roomModuleApi.findById(roomId).map(RoomInfo::name).orElse(null);
            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "TASK:" + taskId);
            doc.addField("doc_type", "TASK");
            doc.addField("entity_id", taskId.toString());
            doc.addField("title", title);
            doc.addField("content", description);
            doc.addField("author_name", assigneeName);
            doc.addField("room_id", roomId.toString());
            doc.addField("room_name", roomName);
            doc.addField("url", "/rooms/" + roomId + "/tasks");
            doc.addField("updated_at", toDate(Instant.now()));
            solrClient.add(doc);
            solrClient.commit();
        } catch (SolrServerException | IOException e) {
            log.error("Failed to index task {}: {}", taskId, e.getMessage());
        }
    }

    public void indexFile(UUID fileId, UUID roomId, String originalName, String contentType, long fileSize) {
        try {
            String roomName = roomModuleApi.findById(roomId).map(RoomInfo::name).orElse(null);
            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "FILE:" + fileId);
            doc.addField("doc_type", "FILE");
            doc.addField("entity_id", fileId.toString());
            doc.addField("title", originalName);
            doc.addField("room_id", roomId.toString());
            doc.addField("room_name", roomName);
            doc.addField("content_type", contentType);
            doc.addField("file_size", fileSize);
            doc.addField("url", "/rooms/" + roomId + "/files");
            doc.addField("created_at", toDate(Instant.now()));
            solrClient.add(doc);
            solrClient.commit();
        } catch (SolrServerException | IOException e) {
            log.error("Failed to index file {}: {}", fileId, e.getMessage());
        }
    }

    public void indexFileWithContent(UUID fileId, UUID roomId, String originalName,
                                      String contentType, long fileSize,
                                      InputStream contentStream) {
        try {
            String roomName = roomModuleApi.findById(roomId).map(RoomInfo::name).orElse(null);

            // Use Tika extraction via Solr's ExtractingRequestHandler
            var request = new org.apache.solr.client.solrj.request.ContentStreamUpdateRequest("/update/extract");
            request.addContentStream(new org.apache.solr.common.util.ContentStreamBase() {
                @Override public String getName() { return originalName; }
                @Override public String getSourceInfo() { return originalName; }
                @Override public String getContentType() { return contentType; }
                @Override public Long getSize() { return fileSize; }
                @Override public InputStream getStream() { return contentStream; }
            });

            request.setParam("literal.id", "FILE:" + fileId);
            request.setParam("literal.doc_type", "FILE");
            request.setParam("literal.entity_id", fileId.toString());
            request.setParam("literal.title", originalName);
            request.setParam("literal.room_id", roomId.toString());
            if (roomName != null) request.setParam("literal.room_name", roomName);
            request.setParam("literal.content_type", contentType);
            request.setParam("literal.file_size", String.valueOf(fileSize));
            request.setParam("literal.url", "/rooms/" + roomId + "/files");
            request.setParam("literal.created_at", Instant.now().toString());

            solrClient.request(request);
            solrClient.commit();
            log.info("Indexed file {} with content extraction", fileId);
        } catch (Exception e) {
            log.warn("Content extraction failed for file {}, indexing metadata only: {}", fileId, e.getMessage());
            indexFile(fileId, roomId, originalName, contentType, fileSize);
        }
    }

    public void deleteDocument(String docType, UUID entityId) {
        try {
            solrClient.deleteById(docType + ":" + entityId);
            solrClient.commit();
        } catch (SolrServerException | IOException e) {
            log.error("Failed to delete {} {} from Solr: {}", docType, entityId, e.getMessage());
        }
    }

    public void deleteUserDocuments(UUID userId) {
        try {
            // Delete the user's own document
            solrClient.deleteById("USER:" + userId.toString());
            solrClient.commit();
            log.info("Deleted Solr documents for user {}", userId);
        } catch (Exception e) {
            log.error("Failed to delete Solr documents for user {}", userId, e);
        }
    }

    // ---- Full re-index ----

    public int reindexAll() {
        int count = 0;
        try {
            // Clear the index first
            solrClient.deleteByQuery("*:*");
            solrClient.commit();

            count += reindexUsers();
            count += reindexRooms();
            count += reindexPosts();
            count += reindexEvents();
            count += reindexFiles();
            count += reindexWikiPages();
            count += reindexTasks();

            solrClient.commit();
            log.info("Full re-index completed: {} documents", count);
        } catch (SolrServerException | IOException e) {
            log.error("Re-index failed: {}", e.getMessage());
        }
        return count;
    }

    private int reindexUsers() throws SolrServerException, IOException {
        var users = userModuleApi.searchUsers("", PageRequest.of(0, 10000));
        int count = 0;
        for (UserInfo user : users.getContent()) {
            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "USER:" + user.id());
            doc.addField("doc_type", "USER");
            doc.addField("entity_id", user.id().toString());
            doc.addField("title", user.displayName());
            doc.addField("content", user.email());
            doc.addField("url", "/users/" + user.id());
            solrClient.add(doc);
            count++;
        }
        return count;
    }

    private int reindexRooms() throws SolrServerException, IOException {
        var rooms = roomModuleApi.searchRooms("", 10000);
        int count = 0;
        for (RoomInfo room : rooms) {
            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "ROOM:" + room.id());
            doc.addField("doc_type", "ROOM");
            doc.addField("entity_id", room.id().toString());
            doc.addField("title", room.name());
            doc.addField("content", room.description());
            doc.addField("url", "/rooms/" + room.id());
            solrClient.add(doc);
            count++;
        }
        return count;
    }

    private int reindexPosts() throws SolrServerException, IOException {
        if (feedModuleApi == null) return 0;
        // Use personal feed for system user (null safe via empty feed)
        // Instead, search with empty query to get all posts
        var posts = feedModuleApi.searchPosts("", 10000, null);
        int count = 0;
        for (FeedPostInfo post : posts) {
            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "POST:" + post.id());
            doc.addField("doc_type", "POST");
            doc.addField("entity_id", post.id().toString());
            doc.addField("title", post.title());
            doc.addField("content", post.content());
            doc.addField("author_name", post.authorName());
            doc.addField("source_type", post.sourceType() != null ? post.sourceType().name() : null);
            doc.addField("url", "/feed?post=" + post.id());
            doc.addField("created_at", toDate(post.publishedAt() != null ? post.publishedAt() : post.createdAt()));
            solrClient.add(doc);
            count++;
        }
        return count;
    }

    private int reindexEvents() throws SolrServerException, IOException {
        if (calendarModuleApi == null) return 0;
        var events = calendarModuleApi.searchEvents("", 10000);
        int count = 0;
        for (EventInfo event : events) {
            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "EVENT:" + event.id());
            doc.addField("doc_type", "EVENT");
            doc.addField("entity_id", event.id().toString());
            doc.addField("title", event.title());
            doc.addField("content", event.description());
            doc.addField("url", "/calendar?event=" + event.id());
            doc.addField("created_at", toDate(event.createdAt()));
            solrClient.add(doc);
            count++;
        }
        return count;
    }

    private int reindexFiles() throws SolrServerException, IOException {
        if (filesModuleApi == null) return 0;
        var files = filesModuleApi.findAllFiles();
        int count = 0;
        for (FileInfo file : files) {
            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "FILE:" + file.id());
            doc.addField("doc_type", "FILE");
            doc.addField("entity_id", file.id().toString());
            doc.addField("title", file.originalName());
            doc.addField("room_id", file.roomId().toString());
            String roomName = roomModuleApi.findById(file.roomId()).map(RoomInfo::name).orElse(null);
            doc.addField("room_name", roomName);
            doc.addField("content_type", file.contentType());
            doc.addField("file_size", file.fileSize());
            doc.addField("url", "/rooms/" + file.roomId() + "/files");
            doc.addField("created_at", toDate(file.createdAt()));
            solrClient.add(doc);
            count++;
        }
        return count;
    }

    private int reindexWikiPages() throws SolrServerException, IOException {
        if (wikiModuleApi == null) return 0;
        var pages = wikiModuleApi.findAllPagesForIndexing();
        int count = 0;
        for (Map<String, Object> page : pages) {
            UUID pageId = (UUID) page.get("id");
            UUID roomId = (UUID) page.get("roomId");
            String title = (String) page.get("title");
            String content = (String) page.get("content");
            String slug = (String) page.get("slug");

            String roomName = roomModuleApi.findById(roomId).map(RoomInfo::name).orElse(null);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "WIKI:" + pageId);
            doc.addField("doc_type", "WIKI");
            doc.addField("entity_id", pageId.toString());
            doc.addField("title", title);
            doc.addField("content", content);
            doc.addField("room_id", roomId.toString());
            doc.addField("room_name", roomName);
            doc.addField("url", "/rooms/" + roomId + "/wiki/" + slug);
            solrClient.add(doc);
            count++;
        }
        return count;
    }

    private int reindexTasks() throws SolrServerException, IOException {
        if (tasksModuleApi == null) return 0;
        var tasks = tasksModuleApi.findAllTasksForIndexing();
        int count = 0;
        for (Map<String, Object> task : tasks) {
            UUID taskId = (UUID) task.get("id");
            UUID roomId = (UUID) task.get("roomId");
            if (roomId == null) continue;
            String title = (String) task.get("title");
            String description = (String) task.get("description");

            String roomName = roomModuleApi.findById(roomId).map(RoomInfo::name).orElse(null);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "TASK:" + taskId);
            doc.addField("doc_type", "TASK");
            doc.addField("entity_id", taskId.toString());
            doc.addField("title", title);
            doc.addField("content", description);
            doc.addField("room_id", roomId.toString());
            doc.addField("room_name", roomName);
            doc.addField("url", "/rooms/" + roomId + "/tasks");
            solrClient.add(doc);
            count++;
        }
        return count;
    }

    private Date toDate(Instant instant) {
        return instant != null ? Date.from(instant) : null;
    }
}
