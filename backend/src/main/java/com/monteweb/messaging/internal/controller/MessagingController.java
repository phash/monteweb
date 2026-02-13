package com.monteweb.messaging.internal.controller;

import com.monteweb.messaging.ConversationInfo;
import com.monteweb.messaging.MessageInfo;
import com.monteweb.messaging.internal.service.MessagingService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.dto.PageResponse;
import com.monteweb.shared.util.SecurityUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages")
@ConditionalOnProperty(prefix = "monteweb.modules.messaging", name = "enabled", havingValue = "true")
public class MessagingController {

    private final MessagingService messagingService;

    public MessagingController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationInfo>>> getConversations() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var conversations = messagingService.findConversationsByUser(userId);
        return ResponseEntity.ok(ApiResponse.ok(conversations));
    }

    @PostMapping("/conversations")
    public ResponseEntity<ApiResponse<ConversationInfo>> startConversation(
            @RequestBody StartConversationRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        ConversationInfo conversation;
        if (request.participantIds().size() == 1 && !request.isGroup()) {
            conversation = messagingService.startDirectConversation(userId, request.participantIds().get(0));
        } else {
            conversation = messagingService.startGroupConversation(userId, request.title(), request.participantIds());
        }
        return ResponseEntity.ok(ApiResponse.ok(conversation));
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ApiResponse<ConversationInfo>> getConversation(
            @PathVariable UUID conversationId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var conversation = messagingService.findConversationById(conversationId, userId)
                .orElseThrow(() -> new com.monteweb.shared.exception.ResourceNotFoundException("Conversation", conversationId));
        return ResponseEntity.ok(ApiResponse.ok(conversation));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<PageResponse<MessageInfo>>> getMessages(
            @PathVariable UUID conversationId,
            @PageableDefault(size = 50) Pageable pageable) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var page = messagingService.getMessages(conversationId, userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<MessageInfo>> sendMessage(
            @PathVariable UUID conversationId,
            @RequestBody SendMessageRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var message = messagingService.sendMessage(conversationId, userId, request.content());
        return ResponseEntity.ok(ApiResponse.ok(message));
    }

    @PutMapping("/conversations/{conversationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID conversationId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        messagingService.markConversationAsRead(conversationId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(@PathVariable UUID conversationId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        messagingService.deleteConversation(conversationId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        long count = messagingService.getTotalUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", count)));
    }

    public record StartConversationRequest(
            String title,
            boolean isGroup,
            List<UUID> participantIds
    ) {
    }

    public record SendMessageRequest(String content) {
    }
}
