package com.monteweb.room.internal.controller;

import com.monteweb.room.internal.service.DiscussionThreadService;
import com.monteweb.room.internal.service.DiscussionThreadService.ReplyInfo;
import com.monteweb.room.internal.service.DiscussionThreadService.ThreadInfo;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.dto.PageResponse;
import com.monteweb.shared.util.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms/{roomId}/threads")
public class DiscussionThreadController {

    private final DiscussionThreadService threadService;

    public DiscussionThreadController(DiscussionThreadService threadService) {
        this.threadService = threadService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ThreadInfo>>> getThreads(
            @PathVariable UUID roomId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var page = threadService.getThreads(roomId, userId, status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ThreadInfo>> createThread(
            @PathVariable UUID roomId,
            @Valid @RequestBody CreateThreadRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var thread = threadService.createThread(roomId, userId, request.title(), request.content());
        return ResponseEntity.ok(ApiResponse.ok(thread));
    }

    @GetMapping("/{threadId}")
    public ResponseEntity<ApiResponse<ThreadInfo>> getThread(
            @PathVariable UUID roomId,
            @PathVariable UUID threadId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var thread = threadService.getThread(roomId, threadId, userId);
        return ResponseEntity.ok(ApiResponse.ok(thread));
    }

    @PutMapping("/{threadId}/archive")
    public ResponseEntity<ApiResponse<ThreadInfo>> archiveThread(
            @PathVariable UUID roomId,
            @PathVariable UUID threadId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var thread = threadService.archiveThread(roomId, threadId, userId);
        return ResponseEntity.ok(ApiResponse.ok(thread));
    }

    @DeleteMapping("/{threadId}")
    public ResponseEntity<ApiResponse<Void>> deleteThread(
            @PathVariable UUID roomId,
            @PathVariable UUID threadId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        threadService.deleteThread(roomId, threadId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/{threadId}/replies")
    public ResponseEntity<ApiResponse<PageResponse<ReplyInfo>>> getReplies(
            @PathVariable UUID roomId,
            @PathVariable UUID threadId,
            @PageableDefault(size = 50) Pageable pageable) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var page = threadService.getReplies(roomId, threadId, userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @PostMapping("/{threadId}/replies")
    public ResponseEntity<ApiResponse<ReplyInfo>> addReply(
            @PathVariable UUID roomId,
            @PathVariable UUID threadId,
            @Valid @RequestBody AddReplyRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var reply = threadService.addReply(roomId, threadId, userId, request.content());
        return ResponseEntity.ok(ApiResponse.ok(reply));
    }

    public record CreateThreadRequest(
            @NotBlank @Size(max = 300) String title,
            String content
    ) {}

    public record AddReplyRequest(
            @NotBlank String content
    ) {}
}
