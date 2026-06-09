package com.monteweb.bookmark.internal.service;

import com.monteweb.bookmark.BookmarkInfo;
import com.monteweb.bookmark.BookmarkModuleApi;
import com.monteweb.bookmark.internal.model.Bookmark;
import com.monteweb.bookmark.internal.repository.BookmarkRepository;
import com.monteweb.calendar.CalendarModuleApi;
import com.monteweb.feed.FeedModuleApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@ConditionalOnProperty(prefix = "monteweb.modules", name = "bookmarks.enabled", havingValue = "true")
@Transactional(readOnly = true)
public class BookmarkService implements BookmarkModuleApi {

    private static final Logger log = LoggerFactory.getLogger(BookmarkService.class);
    private static final Set<String> VALID_TYPES = Set.of("POST", "EVENT", "WIKI_PAGE", "JOB");

    private final BookmarkRepository bookmarkRepository;

    /**
     * Optional source-module facades used to resolve a human-readable title for
     * each bookmark. They are {@code @Autowired(required = false)} because the
     * feed/calendar modules can be disabled independently. JOB and WIKI_PAGE
     * titles are not resolved yet — see {@link #resolveTitle}.
     */
    private final FeedModuleApi feedModuleApi;
    private final CalendarModuleApi calendarModuleApi;

    public BookmarkService(BookmarkRepository bookmarkRepository,
                           @Autowired(required = false) FeedModuleApi feedModuleApi,
                           @Autowired(required = false) CalendarModuleApi calendarModuleApi) {
        this.bookmarkRepository = bookmarkRepository;
        this.feedModuleApi = feedModuleApi;
        this.calendarModuleApi = calendarModuleApi;
    }

    @Transactional
    public BookmarkInfo toggle(UUID userId, String contentType, UUID contentId) {
        validateContentType(contentType);
        var existing = bookmarkRepository.findByUserIdAndContentTypeAndContentId(userId, contentType, contentId);
        if (existing.isPresent()) {
            bookmarkRepository.delete(existing.get());
            return null; // removed
        } else {
            var bookmark = new Bookmark();
            bookmark.setUserId(userId);
            bookmark.setContentType(contentType);
            bookmark.setContentId(contentId);
            return toInfo(bookmarkRepository.save(bookmark));
        }
    }

    @Override
    public boolean isBookmarked(UUID userId, String contentType, UUID contentId) {
        return bookmarkRepository.existsByUserIdAndContentTypeAndContentId(userId, contentType, contentId);
    }

    @Override
    public Set<UUID> getBookmarkedIds(UUID userId, String contentType) {
        return new HashSet<>(bookmarkRepository.findContentIdsByUserIdAndContentType(userId, contentType));
    }

    @Override
    public Page<BookmarkInfo> getBookmarks(UUID userId, String contentType, Pageable pageable) {
        if (contentType != null && !contentType.isEmpty()) {
            return bookmarkRepository.findByUserIdAndContentTypeOrderByCreatedAtDesc(userId, contentType, pageable)
                    .map(this::toInfo);
        }
        return bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toInfo);
    }

    @Override
    public Map<String, Object> exportUserData(UUID userId) {
        var bookmarks = bookmarkRepository.findByUserId(userId).stream()
                .map(this::toInfo)
                .toList();
        return Map.of("bookmarks", bookmarks);
    }

    @Transactional
    public void cleanupUserData(UUID userId) {
        bookmarkRepository.deleteByUserId(userId);
        log.info("Cleaned up bookmarks for deleted user {}", userId);
    }

    private void validateContentType(String contentType) {
        if (!VALID_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Invalid content type: " + contentType + ". Must be one of: " + VALID_TYPES);
        }
    }

    private BookmarkInfo toInfo(Bookmark bookmark) {
        return new BookmarkInfo(
                bookmark.getId(),
                bookmark.getUserId(),
                bookmark.getContentType(),
                bookmark.getContentId(),
                resolveTitle(bookmark.getContentType(), bookmark.getContentId()),
                bookmark.getCreatedAt()
        );
    }

    /**
     * Best-effort resolution of a display title from the source module.
     *
     * <p>Currently supports POST (feed) and EVENT (calendar), whose public
     * facades expose a single-item lookup with a title. JOB and WIKI_PAGE
     * return {@code null} because {@code JobboardModuleApi}/{@code WikiModuleApi}
     * do not yet expose a by-id title lookup (cross-module API gap — see flags).
     * Returns {@code null} on any failure so listing a bookmark never breaks
     * because a source item is missing or its module is disabled.
     */
    private String resolveTitle(String contentType, UUID contentId) {
        try {
            return switch (contentType) {
                case "POST" -> feedModuleApi == null ? null
                        : feedModuleApi.findPostById(contentId).map(p -> p.title()).orElse(null);
                case "EVENT" -> calendarModuleApi == null ? null
                        : calendarModuleApi.findById(contentId).map(e -> e.title()).orElse(null);
                default -> null; // JOB, WIKI_PAGE: no by-id title facade yet
            };
        } catch (RuntimeException ex) {
            log.debug("Could not resolve title for {} {}: {}", contentType, contentId, ex.getMessage());
            return null;
        }
    }
}
