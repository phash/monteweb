package com.monteweb.bookmark.internal.service;

import com.monteweb.bookmark.BookmarkInfo;
import com.monteweb.bookmark.BookmarkModuleApi;
import com.monteweb.bookmark.internal.model.Bookmark;
import com.monteweb.bookmark.internal.repository.BookmarkRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class BookmarkService implements BookmarkModuleApi {

    private static final Set<String> VALID_TYPES = Set.of("POST", "EVENT", "WIKI_PAGE", "JOB");

    private final BookmarkRepository bookmarkRepository;

    public BookmarkService(BookmarkRepository bookmarkRepository) {
        this.bookmarkRepository = bookmarkRepository;
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
                bookmark.getCreatedAt()
        );
    }
}
