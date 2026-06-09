package com.monteweb.search.internal.service;

import com.monteweb.calendar.CalendarModuleApi;
import com.monteweb.calendar.EventInfo;
import com.monteweb.feed.FeedModuleApi;
import com.monteweb.feed.FeedPostInfo;
import com.monteweb.files.FileInfo;
import com.monteweb.files.FilesModuleApi;
import com.monteweb.room.RoomInfo;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.search.SearchResult;
import com.monteweb.tasks.TasksModuleApi;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import com.monteweb.wiki.WikiModuleApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SearchService {

    private final UserModuleApi userModuleApi;
    private final RoomModuleApi roomModuleApi;
    private final FeedModuleApi feedModuleApi;
    private final CalendarModuleApi calendarModuleApi;
    private final FilesModuleApi filesModuleApi;
    private final WikiModuleApi wikiModuleApi;
    private final TasksModuleApi tasksModuleApi;
    private final SolrSearchService solrSearchService;

    public SearchService(UserModuleApi userModuleApi,
                         RoomModuleApi roomModuleApi,
                         @Autowired(required = false) FeedModuleApi feedModuleApi,
                         @Autowired(required = false) CalendarModuleApi calendarModuleApi,
                         @Autowired(required = false) FilesModuleApi filesModuleApi,
                         @Autowired(required = false) WikiModuleApi wikiModuleApi,
                         @Autowired(required = false) TasksModuleApi tasksModuleApi,
                         @Autowired(required = false) SolrSearchService solrSearchService) {
        this.userModuleApi = userModuleApi;
        this.roomModuleApi = roomModuleApi;
        this.feedModuleApi = feedModuleApi;
        this.calendarModuleApi = calendarModuleApi;
        this.filesModuleApi = filesModuleApi;
        this.wikiModuleApi = wikiModuleApi;
        this.tasksModuleApi = tasksModuleApi;
        this.solrSearchService = solrSearchService;
    }

    public List<SearchResult> search(String query, String type, int limit, UUID userId) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }

        // Delegate to Solr if available
        if (solrSearchService != null) {
            return solrSearchService.search(query.trim(), type, limit, userId);
        }

        // Fallback: DB-based search
        return searchDatabase(query.trim(), type, limit, userId);
    }

    private List<SearchResult> searchDatabase(String q, String type, int limit, UUID userId) {
        List<SearchResult> results = new ArrayList<>();

        if ("ALL".equals(type) || "USER".equals(type)) {
            results.addAll(searchUsers(q, limit));
        }
        if ("ALL".equals(type) || "ROOM".equals(type)) {
            results.addAll(searchRooms(q, limit));
        }
        if ("ALL".equals(type) || "POST".equals(type)) {
            results.addAll(searchPosts(q, limit, userId));
        }
        if ("ALL".equals(type) || "EVENT".equals(type)) {
            results.addAll(searchEvents(q, limit));
        }
        // Room-scoped types: only resolve the user's accessible rooms when needed.
        if (isRoomScopedRequested(type)) {
            Set<UUID> accessibleRoomIds = resolveAccessibleRoomIds(userId);
            if ("ALL".equals(type) || "FILE".equals(type)) {
                results.addAll(searchFiles(q, limit, accessibleRoomIds));
            }
            if ("ALL".equals(type) || "WIKI".equals(type)) {
                results.addAll(searchWiki(q, limit, accessibleRoomIds));
            }
            if ("ALL".equals(type) || "TASK".equals(type)) {
                results.addAll(searchTasks(q, limit, accessibleRoomIds));
            }
        }

        // Sort: exact title matches first, then by timestamp descending
        String lowerQ = q.toLowerCase();
        results.sort(Comparator
                .<SearchResult, Boolean>comparing(r -> !r.title().toLowerCase().startsWith(lowerQ))
                .thenComparing(r -> r.timestamp() == null ? java.time.Instant.EPOCH : r.timestamp(),
                        Comparator.reverseOrder()));

        // Limit total results
        if (results.size() > limit) {
            results = results.subList(0, limit);
        }

        return results;
    }

    private List<SearchResult> searchUsers(String query, int limit) {
        try {
            var page = userModuleApi.searchUsers(query, PageRequest.of(0, limit));
            return page.getContent().stream()
                    .map(this::toUserResult)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SearchResult> searchRooms(String query, int limit) {
        try {
            return roomModuleApi.searchRooms(query, limit).stream()
                    .map(this::toRoomResult)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SearchResult> searchPosts(String query, int limit, UUID userId) {
        if (feedModuleApi == null) return List.of();
        try {
            return feedModuleApi.searchPosts(query, limit, userId).stream()
                    .map(this::toPostResult)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SearchResult> searchEvents(String query, int limit) {
        if (calendarModuleApi == null) return List.of();
        try {
            return calendarModuleApi.searchEvents(query, limit).stream()
                    .map(this::toEventResult)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private boolean isRoomScopedRequested(String type) {
        return "ALL".equals(type) || "FILE".equals(type) || "WIKI".equals(type) || "TASK".equals(type);
    }

    /**
     * Resolves the set of room IDs the user may see. Room-scoped fallback results
     * (FILE, WIKI, TASK) must be constrained to these rooms — mirroring the Solr
     * access filter in {@link SolrSearchService}. Fails closed (empty set) on error
     * or when no user is given.
     */
    private Set<UUID> resolveAccessibleRoomIds(UUID userId) {
        if (userId == null) return Set.of();
        try {
            return roomModuleApi.findByUserId(userId).stream()
                    .map(RoomInfo::id)
                    .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        } catch (Exception e) {
            return Set.of();
        }
    }

    private List<SearchResult> searchFiles(String query, int limit, Set<UUID> accessibleRoomIds) {
        if (filesModuleApi == null || accessibleRoomIds.isEmpty()) return List.of();
        String lowerQ = query.toLowerCase();
        try {
            return filesModuleApi.findAllFiles().stream()
                    .filter(f -> f.roomId() != null && accessibleRoomIds.contains(f.roomId()))
                    .filter(f -> f.originalName() != null
                            && f.originalName().toLowerCase().contains(lowerQ))
                    .limit(limit)
                    .map(this::toFileResult)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SearchResult> searchWiki(String query, int limit, Set<UUID> accessibleRoomIds) {
        if (wikiModuleApi == null || accessibleRoomIds.isEmpty()) return List.of();
        String lowerQ = query.toLowerCase();
        try {
            return wikiModuleApi.findAllPagesForIndexing().stream()
                    .filter(p -> {
                        UUID roomId = (UUID) p.get("roomId");
                        return roomId != null && accessibleRoomIds.contains(roomId);
                    })
                    .filter(p -> matchesAny(lowerQ, (String) p.get("title"), (String) p.get("content")))
                    .limit(limit)
                    .map(this::toWikiResult)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<SearchResult> searchTasks(String query, int limit, Set<UUID> accessibleRoomIds) {
        if (tasksModuleApi == null || accessibleRoomIds.isEmpty()) return List.of();
        String lowerQ = query.toLowerCase();
        try {
            return tasksModuleApi.findAllTasksForIndexing().stream()
                    .filter(t -> {
                        UUID roomId = (UUID) t.get("roomId");
                        return roomId != null && accessibleRoomIds.contains(roomId);
                    })
                    .filter(t -> matchesAny(lowerQ, (String) t.get("title"), (String) t.get("description")))
                    .limit(limit)
                    .map(this::toTaskResult)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private boolean matchesAny(String lowerQ, String... fields) {
        for (String field : fields) {
            if (field != null && field.toLowerCase().contains(lowerQ)) {
                return true;
            }
        }
        return false;
    }

    private SearchResult toUserResult(UserInfo user) {
        return new SearchResult(
                user.id(),
                "USER",
                user.displayName(),
                user.email(),
                user.role() != null ? user.role().name() : null,
                "/users/" + user.id(),
                null
        );
    }

    private SearchResult toRoomResult(RoomInfo room) {
        return new SearchResult(
                room.id(),
                "ROOM",
                room.name(),
                room.type() + " - " + room.memberCount() + " Mitglieder",
                room.description(),
                "/rooms/" + room.id(),
                null
        );
    }

    private SearchResult toPostResult(FeedPostInfo post) {
        String title = post.title() != null ? post.title() : truncate(post.content(), 80);
        String snippet = truncate(post.content(), 150);
        return new SearchResult(
                post.id(),
                "POST",
                title,
                post.sourceName(),
                snippet,
                "/feed?post=" + post.id(),
                post.publishedAt() != null ? post.publishedAt() : post.createdAt()
        );
    }

    private SearchResult toEventResult(EventInfo event) {
        String subtitle = event.location() != null ? event.location() : event.scopeName();
        return new SearchResult(
                event.id(),
                "EVENT",
                event.title(),
                subtitle,
                event.description() != null ? truncate(event.description(), 150) : null,
                "/calendar?event=" + event.id(),
                event.createdAt()
        );
    }

    private SearchResult toFileResult(FileInfo file) {
        String roomName = roomName(file.roomId());
        return new SearchResult(
                file.id(),
                "FILE",
                file.originalName(),
                roomName,
                file.contentType(),
                "/rooms/" + file.roomId() + "/files",
                file.createdAt()
        );
    }

    private SearchResult toWikiResult(Map<String, Object> page) {
        UUID pageId = (UUID) page.get("id");
        UUID roomId = (UUID) page.get("roomId");
        String title = (String) page.get("title");
        String content = (String) page.get("content");
        String slug = (String) page.get("slug");
        String roomName = roomName(roomId);
        return new SearchResult(
                pageId,
                "WIKI",
                title,
                roomName != null ? "Wiki - " + roomName : "Wiki",
                truncate(content, 150),
                "/rooms/" + roomId + "/wiki/" + slug,
                null
        );
    }

    private SearchResult toTaskResult(Map<String, Object> task) {
        UUID taskId = (UUID) task.get("id");
        UUID roomId = (UUID) task.get("roomId");
        String title = (String) task.get("title");
        String description = (String) task.get("description");
        String roomName = roomName(roomId);
        return new SearchResult(
                taskId,
                "TASK",
                title,
                roomName != null ? "Aufgabe - " + roomName : "Aufgabe",
                truncate(description, 150),
                "/rooms/" + roomId + "/tasks",
                null
        );
    }

    private String roomName(UUID roomId) {
        if (roomId == null) return null;
        try {
            return roomModuleApi.findById(roomId).map(RoomInfo::name).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
