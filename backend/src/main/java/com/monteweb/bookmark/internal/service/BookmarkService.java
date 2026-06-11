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
                // Resolve the title ONLY for the bookmark's owner and ONLY through
                // access-checked lookups, so a restricted source item never leaks
                // its title to a user who may not see it (broken object-level
                // authorization, see resolveTitle).
                resolveTitle(bookmark.getContentType(), bookmark.getContentId(), bookmark.getUserId()),
                bookmark.getCreatedAt()
        );
    }

    /**
     * Best-effort, access-checked resolution of a display title from the source module.
     *
     * <p>Security: the denormalized {@link BookmarkInfo#title()} is a convenience
     * label, but it must never reveal the title of content the bookmark owner is
     * not allowed to see. A bookmark can hold ANY caller-supplied {@code contentId}
     * (see {@link #toggle}, which only validates the content <em>type</em>, never
     * read access), so an authenticated PARENT/STUDENT could otherwise bookmark a
     * post in a private room they are not a member of and then read its title back
     * here. To prevent that, titles are resolved exclusively through the source
     * module's <em>access-checked</em> facade, passing {@code ownerId}; if the owner
     * lacks access the source returns {@link Optional#empty()} (or throws
     * {@link com.monteweb.shared.exception.ForbiddenException}, caught below) and we
     * fall back to a {@code null} title.
     *
     * <p>The raw, unfiltered {@code FeedModuleApi.findPostById} /
     * {@code CalendarModuleApi.findById} lookups are deliberately NOT used here —
     * they perform no authorization and leaked restricted titles previously
     * (broken object-level authorization). Until the feed and calendar modules
     * expose <em>access-checked</em> by-id facades
     * ({@code FeedModuleApi.findPostByIdForUser(postId, userId)} delegating to
     * {@code FeedService.verifyPostReadAccess}, and
     * {@code CalendarModuleApi.findByIdForUser(eventId, userId)} delegating to
     * {@code CalendarService.getEvent}), POST and EVENT titles are intentionally
     * left {@code null} rather than risk leaking a restricted title. This is a
     * cross-module API gap that is out of this module's scope to add — see flags.
     *
     * <p>JOB and WIKI_PAGE likewise return {@code null} because their modules do
     * not yet expose a by-id title lookup. {@code title} is documented as a
     * best-effort, nullable label on {@link BookmarkInfo}, so a {@code null} here
     * never breaks listing a bookmark.
     *
     * @param ownerId the user who owns the bookmark; access must be evaluated for
     *                them once the access-checked facades exist
     */
    private String resolveTitle(String contentType, UUID contentId, UUID ownerId) {
        // SECURITY: do not resolve POST/EVENT titles through the unfiltered
        // by-id facades (feedModuleApi.findPostById / calendarModuleApi.findById)
        // — doing so leaks titles of content the owner may not see. Restore
        // per-type resolution only via access-checked facades (see Javadoc).
        return null;
    }
}
