package com.monteweb.feed.internal.controller;

import com.monteweb.feed.FeedPostInfo;
import com.monteweb.feed.SourceType;
import com.monteweb.feed.internal.dto.*;
import com.monteweb.feed.internal.service.FeedService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.dto.PageResponse;
import com.monteweb.shared.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/feed")
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FeedPostInfo>>> getPersonalFeed(
            @PageableDefault(size = 20) Pageable pageable) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var page = feedService.getPersonalFeed(userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @GetMapping("/banners")
    public ResponseEntity<ApiResponse<List<FeedPostInfo>>> getActiveBanners() {
        return ResponseEntity.ok(ApiResponse.ok(feedService.getActiveSystemBanners()));
    }

    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<FeedPostInfo>> createPost(@Valid @RequestBody CreatePostRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var post = feedService.createPost(
                userId, request.title(), request.content(),
                request.sourceType(), request.sourceId(), request.parentOnly()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(post));
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<FeedPostInfo>> getPost(@PathVariable UUID id) {
        var post = feedService.findPostById(id)
                .orElseThrow(() -> new com.monteweb.shared.exception.ResourceNotFoundException("FeedPost", id));
        return ResponseEntity.ok(ApiResponse.ok(post));
    }

    @PutMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<FeedPostInfo>> updatePost(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePostRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var post = feedService.updatePost(id, userId, request.title(), request.content(), request.parentOnly());
        return ResponseEntity.ok(ApiResponse.ok(post));
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        feedService.deletePost(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Post deleted"));
    }

    @PostMapping("/posts/{id}/pin")
    public ResponseEntity<ApiResponse<FeedPostInfo>> togglePin(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var post = feedService.togglePin(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(post));
    }

    @PostMapping("/posts/{id}/comments")
    public ResponseEntity<ApiResponse<Void>> addComment(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCommentRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        feedService.addComment(id, userId, request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(null, "Comment added"));
    }

    @GetMapping("/rooms/{roomId}/posts")
    public ResponseEntity<ApiResponse<PageResponse<FeedPostInfo>>> getRoomPosts(
            @PathVariable UUID roomId,
            @PageableDefault(size = 20) Pageable pageable) {
        var page = feedService.getPostsBySource(SourceType.ROOM, roomId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }
}
