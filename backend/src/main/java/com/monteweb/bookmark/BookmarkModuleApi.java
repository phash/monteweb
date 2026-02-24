package com.monteweb.bookmark;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Public API: Facade interface for the bookmark module.
 */
public interface BookmarkModuleApi {

    boolean isBookmarked(UUID userId, String contentType, UUID contentId);

    Set<UUID> getBookmarkedIds(UUID userId, String contentType);

    Page<BookmarkInfo> getBookmarks(UUID userId, String contentType, Pageable pageable);

    /**
     * DSGVO: Export all bookmark-related data for a user.
     */
    Map<String, Object> exportUserData(UUID userId);
}
