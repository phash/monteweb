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
        // Resolve the user's accessible rooms once: needed for POST/EVENT scoping as well as the
        // room-scoped types (FILE/WIKI/TASK).
        Set<UUID> accessibleRoomIds = resolveAccessibleRoomIds(userId);
        if ("ALL".equals(type) || "POST".equals(type)) {
            results.addAll(searchPosts(q, limit, userId, accessibleRoomIds));
        }
        if ("ALL".equals(type) || "EVENT".equals(type)) {
            results.addAll(searchEvents(q, limit, userId, accessibleRoomIds));
        }
        // Room-scoped types: reuse the accessible rooms resolved above.
        if (isRoomScopedRequested(type)) {
            if ("ALL".equals(type) || "FILE".equals(type)) {
                results.addAll(searchFiles(q, limit, accessibleRoomIds, userId));
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

    /**
     * Feed's {@code searchPosts} already filters per-user targeted posts (target_user_ids), but
     * NOT room membership or parentOnly. We re-apply those here so the DB fallback mirrors the
     * Solr access filter: ROOM-scoped posts require membership, and parentOnly posts are hidden
     * from students.
     */
    private List<SearchResult> searchPosts(String query, int limit, UUID userId, Set<UUID> accessibleRoomIds) {
        if (feedModuleApi == null) return List.of();
        boolean isStudent = isStudent(userId);
        try {
            return feedModuleApi.searchPosts(query, limit, userId).stream()
                    .filter(p -> canSeePost(p, accessibleRoomIds, isStudent))
                    .map(this::toPostResult)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private boolean canSeePost(FeedPostInfo post, Set<UUID> accessibleRoomIds, boolean isStudent) {
        // ROOM-scoped posts are only visible to room members.
        if (post.sourceType() == com.monteweb.feed.SourceType.ROOM) {
            if (post.sourceId() == null || !accessibleRoomIds.contains(post.sourceId())) {
                return false;
            }
        }
        // parentOnly posts must never be shown to students.
        if (post.parentOnly() && isStudent) {
            return false;
        }
        return true;
    }

    /**
     * Calendar's {@code searchEvents} returns events of every scope. We restrict ROOM-scoped
     * events to the user's rooms and SECTION-scoped events to the user's sections; SCHOOL events
     * remain visible to all. Mirrors the Solr EVENT access filter.
     */
    private List<SearchResult> searchEvents(String query, int limit, UUID userId, Set<UUID> accessibleRoomIds) {
        if (calendarModuleApi == null) return List.of();
        Set<UUID> accessibleSectionIds = resolveAccessibleSectionIds(userId);
        try {
            return calendarModuleApi.searchEvents(query, limit).stream()
                    .filter(e -> canSeeEvent(e, accessibleRoomIds, accessibleSectionIds))
                    .map(this::toEventResult)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private boolean canSeeEvent(EventInfo event, Set<UUID> accessibleRoomIds, Set<UUID> accessibleSectionIds) {
        if (event.scope() == null) {
            return false; // unknown scope: fail closed
        }
        return switch (event.scope()) {
            case SCHOOL -> true;
            case ROOM -> event.scopeId() != null && accessibleRoomIds.contains(event.scopeId());
            case SECTION -> event.scopeId() != null && accessibleSectionIds.contains(event.scopeId());
        };
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

    /** Sections the user belongs to, derived from the sections of the rooms they are a member of. */
    private Set<UUID> resolveAccessibleSectionIds(UUID userId) {
        if (userId == null) return Set.of();
        try {
            return roomModuleApi.findByUserId(userId).stream()
                    .map(RoomInfo::sectionId)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        } catch (Exception e) {
            return Set.of();
        }
    }

    private boolean isStudent(UUID userId) {
        if (userId == null) return false;
        try {
            return userModuleApi.findById(userId)
                    .map(UserInfo::role)
                    .map(r -> r == com.monteweb.user.UserRole.STUDENT)
                    .orElse(false);
        } catch (Exception e) {
            return true; // fail closed: hide parentOnly content
        }
    }

    /**
     * File audiences the user may see, mirroring {@code FileService.getAllowedAudiences} via the
     * global role (per-room LEADER/PARENT_MEMBER room roles are not resolved here — see FLAG).
     */
    private Set<String> allowedAudiencesForUser(UUID userId) {
        if (userId == null) return Set.of("ALL");
        var role = userModuleApi.findById(userId).map(UserInfo::role).orElse(null);
        if (role == com.monteweb.user.UserRole.TEACHER
                || role == com.monteweb.user.UserRole.SUPERADMIN
                || role == com.monteweb.user.UserRole.SECTION_ADMIN) {
            return Set.of("ALL", "PARENTS_ONLY", "STUDENTS_ONLY");
        }
        if (role == com.monteweb.user.UserRole.PARENT) {
            return Set.of("ALL", "PARENTS_ONLY");
        }
        if (role == com.monteweb.user.UserRole.STUDENT) {
            return Set.of("ALL", "STUDENTS_ONLY");
        }
        return Set.of("ALL");
    }

    private List<SearchResult> searchFiles(String query, int limit, Set<UUID> accessibleRoomIds, UUID userId) {
        if (filesModuleApi == null || accessibleRoomIds.isEmpty()) return List.of();
        String lowerQ = query.toLowerCase();
        Set<String> allowedAudiences = allowedAudiencesForUser(userId);
        try {
            return filesModuleApi.findAllFiles().stream()
                    .filter(f -> f.roomId() != null && accessibleRoomIds.contains(f.roomId()))
                    .filter(f -> f.originalName() != null
                            && f.originalName().toLowerCase().contains(lowerQ))
                    // Mirror FileService.listFiles audience gating. We use the file's own audience
                    // (folder-inherited audience is not exposed cross-module — see FLAG).
                    .filter(f -> allowedAudiences.contains(
                            f.audience() == null || f.audience().isBlank() ? "ALL" : f.audience()))
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
