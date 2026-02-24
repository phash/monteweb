package com.monteweb.bookmark.internal.controller;

import com.monteweb.bookmark.BookmarkInfo;
import com.monteweb.bookmark.internal.service.BookmarkService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.dto.PageResponse;
import com.monteweb.shared.util.SecurityUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggle(@RequestBody Map<String, String> body) {
        var userId = SecurityUtils.requireCurrentUserId();
        var contentType = body.get("contentType");
        var contentIdStr = body.get("contentId");

        if (contentType == null || contentIdStr == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("contentType and contentId are required"));
        }

        UUID contentId;
        try {
            contentId = UUID.fromString(contentIdStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid contentId"));
        }

        var result = bookmarkService.toggle(userId, contentType, contentId);
        var bookmarked = result != null;
        return ResponseEntity.ok(ApiResponse.ok(Map.of("bookmarked", bookmarked)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BookmarkInfo>>> list(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var userId = SecurityUtils.requireCurrentUserId();
        var result = bookmarkService.getBookmarks(userId, type, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> check(
            @RequestParam String contentType,
            @RequestParam UUID contentId) {
        var userId = SecurityUtils.requireCurrentUserId();
        var bookmarked = bookmarkService.isBookmarked(userId, contentType, contentId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("bookmarked", bookmarked)));
    }

    @GetMapping("/ids")
    public ResponseEntity<ApiResponse<Set<UUID>>> getBookmarkedIds(@RequestParam String contentType) {
        var userId = SecurityUtils.requireCurrentUserId();
        var ids = bookmarkService.getBookmarkedIds(userId, contentType);
        return ResponseEntity.ok(ApiResponse.ok(ids));
    }
}
