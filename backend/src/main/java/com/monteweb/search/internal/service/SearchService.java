package com.monteweb.search.internal.service;

import com.monteweb.calendar.CalendarModuleApi;
import com.monteweb.calendar.EventInfo;
import com.monteweb.feed.FeedModuleApi;
import com.monteweb.feed.FeedPostInfo;
import com.monteweb.room.RoomInfo;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.search.SearchResult;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class SearchService {

    private final UserModuleApi userModuleApi;
    private final RoomModuleApi roomModuleApi;
    private final FeedModuleApi feedModuleApi;
    private final CalendarModuleApi calendarModuleApi;
    private final SolrSearchService solrSearchService;

    public SearchService(UserModuleApi userModuleApi,
                         RoomModuleApi roomModuleApi,
                         @Autowired(required = false) FeedModuleApi feedModuleApi,
                         @Autowired(required = false) CalendarModuleApi calendarModuleApi,
                         @Autowired(required = false) SolrSearchService solrSearchService) {
        this.userModuleApi = userModuleApi;
        this.roomModuleApi = roomModuleApi;
        this.feedModuleApi = feedModuleApi;
        this.calendarModuleApi = calendarModuleApi;
        this.solrSearchService = solrSearchService;
    }

    public List<SearchResult> search(String query, String type, int limit, UUID userId) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }

        // Delegate to Solr if available
        if (solrSearchService != null) {
            return solrSearchService.search(query.trim(), type, limit);
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

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
