package com.monteweb.feed.internal.controller;

import com.monteweb.feed.FeedPostInfo;
import com.monteweb.feed.LinkPreviewInfo;
import com.monteweb.feed.PollInfo;
import com.monteweb.feed.SourceType;
import com.monteweb.feed.internal.dto.*;
import com.monteweb.feed.internal.service.FeedService;
import com.monteweb.feed.internal.service.LinkPreviewService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.dto.PageResponse;
import com.monteweb.shared.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/feed")
public class FeedController {

    private final FeedService feedService;
    private final LinkPreviewService linkPreviewService;

    public FeedController(FeedService feedService, LinkPreviewService linkPreviewService) {
        this.feedService = feedService;
        this.linkPreviewService = linkPreviewService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FeedPostInfo>>> getPersonalFeed(
            @PageableDefault(size = 20) Pageable pageable) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var page = feedService.getPersonalFeed(userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(enrichPage(page, userId))));
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
                request.sourceType(), request.sourceId(), request.parentOnly(),
                request.poll()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(post));
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<FeedPostInfo>> getPost(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var post = feedService.findPostById(id)
                .orElseThrow(() -> new com.monteweb.shared.exception.ResourceNotFoundException("FeedPost", id));
        return ResponseEntity.ok(ApiResponse.ok(enrichPost(post, userId)));
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

    @GetMapping("/posts/{id}/comments")
    public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> getComments(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var page = feedService.getComments(id, pageable);
        var enriched = page.map(c -> enrichComment(c, userId));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(enriched)));
    }

    @PostMapping("/posts/{id}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCommentRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var comment = feedService.addComment(id, userId, request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(comment));
    }

    // --- Reactions ---

    @PostMapping("/posts/{id}/reactions")
    public ResponseEntity<ApiResponse<List<FeedPostInfo.ReactionSummary>>> togglePostReaction(
            @PathVariable UUID id,
            @Valid @RequestBody ReactionRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        feedService.togglePostReaction(id, userId, request.emoji());
        var reactions = feedService.getPostReactions(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(reactions));
    }

    @PostMapping("/comments/{id}/reactions")
    public ResponseEntity<ApiResponse<List<CommentResponse.ReactionSummary>>> toggleCommentReaction(
            @PathVariable UUID id,
            @Valid @RequestBody ReactionRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        feedService.toggleCommentReaction(id, userId, request.emoji());
        var reactions = feedService.getCommentReactions(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(reactions));
    }

    // --- Polls ---

    @PostMapping("/posts/{id}/poll/vote")
    public ResponseEntity<ApiResponse<PollInfo>> votePoll(
            @PathVariable UUID id,
            @RequestBody VotePollRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var poll = feedService.voteFeedPoll(id, userId, request.optionIds());
        return ResponseEntity.ok(ApiResponse.ok(poll));
    }

    @PostMapping("/posts/{id}/poll/close")
    public ResponseEntity<ApiResponse<PollInfo>> closePoll(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var poll = feedService.closeFeedPoll(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(poll));
    }

    // --- Attachments ---

    @PostMapping("/posts/{id}/attachments")
    public ResponseEntity<ApiResponse<FeedPostInfo>> uploadAttachments(
            @PathVariable UUID id,
            @RequestParam("files") List<MultipartFile> files) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var post = feedService.addAttachments(id, userId, files);
        return ResponseEntity.ok(ApiResponse.ok(post));
    }

    @GetMapping("/attachments/{id}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable UUID id) {
        SecurityUtils.requireCurrentUserId();
        var attachment = feedService.getAttachment(id);
        var stream = feedService.downloadAttachment(attachment.getFileUrl());
        String contentType = attachment.getFileType() != null ? attachment.getFileType() : "application/octet-stream";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"")
                .body(new InputStreamResource(stream));
    }

    @DeleteMapping("/attachments/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        feedService.deleteAttachment(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Attachment deleted"));
    }

    // --- Link preview ---

    @GetMapping("/link-preview")
    public ResponseEntity<ApiResponse<LinkPreviewInfo>> getLinkPreview(@RequestParam String url) {
        var preview = linkPreviewService.fetchPreview(url);
        if (preview == null) {
            return ResponseEntity.ok(ApiResponse.ok(null));
        }
        return ResponseEntity.ok(ApiResponse.ok(preview));
    }

    // --- Room posts ---

    @GetMapping("/rooms/{roomId}/posts")
    public ResponseEntity<ApiResponse<PageResponse<FeedPostInfo>>> getRoomPosts(
            @PathVariable UUID roomId,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var page = feedService.getPostsBySource(SourceType.ROOM, roomId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(enrichPage(page, userId))));
    }

    @PostMapping("/rooms/{roomId}/posts")
    public ResponseEntity<ApiResponse<FeedPostInfo>> createRoomPost(
            @PathVariable UUID roomId,
            @Valid @RequestBody CreateRoomPostRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var post = feedService.createPost(
                userId, request.title(), request.content(),
                SourceType.ROOM, roomId, false
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(post));
    }

    // --- Helper: enrich posts/comments with reactions for current user ---

    private Page<FeedPostInfo> enrichPage(Page<FeedPostInfo> page, UUID userId) {
        return page.map(post -> enrichPost(post, userId));
    }

    private FeedPostInfo enrichPost(FeedPostInfo post, UUID userId) {
        var reactions = feedService.getPostReactions(post.id(), userId);
        return new FeedPostInfo(
                post.id(), post.authorId(), post.authorName(), post.title(), post.content(),
                post.sourceType(), post.sourceId(), post.sourceName(), post.pinned(),
                post.parentOnly(), post.commentCount(), post.attachments(),
                reactions, post.poll(), post.publishedAt(), post.createdAt()
        );
    }

    private CommentResponse enrichComment(CommentResponse comment, UUID userId) {
        var reactions = feedService.getCommentReactions(comment.id(), userId);
        return new CommentResponse(
                comment.id(), comment.postId(), comment.authorId(), comment.authorName(),
                comment.content(), reactions, comment.createdAt()
        );
    }
}
