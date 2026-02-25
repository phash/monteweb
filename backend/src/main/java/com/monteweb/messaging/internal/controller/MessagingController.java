package com.monteweb.messaging.internal.controller;

import com.monteweb.feed.PollInfo;
import com.monteweb.messaging.ConversationInfo;
import com.monteweb.messaging.internal.dto.CreateMessagePollRequest;
import com.monteweb.messaging.MessageInfo;
import com.monteweb.messaging.internal.service.MessagingService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.dto.PageResponse;
import com.monteweb.shared.util.SecurityUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value = "/conversations/{conversationId}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MessageInfo>> sendMessageMultipart(
            @PathVariable UUID conversationId,
            @RequestPart(value = "content", required = false) String content,
            @RequestPart(value = "replyToId", required = false) String replyToId,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart(value = "attachment", required = false) MultipartFile attachment,
            @RequestPart(value = "linkedFileId", required = false) String linkedFileId,
            @RequestPart(value = "linkedRoomId", required = false) String linkedRoomId,
            @RequestPart(value = "linkedFileName", required = false) String linkedFileName) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        UUID replyTo = replyToId != null && !replyToId.isBlank() ? UUID.fromString(replyToId) : null;
        UUID fileId = linkedFileId != null && !linkedFileId.isBlank() ? UUID.fromString(linkedFileId) : null;
        UUID roomId = linkedRoomId != null && !linkedRoomId.isBlank() ? UUID.fromString(linkedRoomId) : null;
        var message = messagingService.sendMessage(conversationId, userId, content, replyTo, image, attachment, fileId, roomId, linkedFileName);
        return ResponseEntity.ok(ApiResponse.ok(message));
    }

    @PostMapping(value = "/conversations/{conversationId}/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<MessageInfo>> sendMessageJson(
            @PathVariable UUID conversationId,
            @RequestBody SendMessageRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        UUID replyTo = request.replyToId() != null ? UUID.fromString(request.replyToId()) : null;
        UUID fileId = request.linkedFileId() != null ? UUID.fromString(request.linkedFileId()) : null;
        UUID roomId = request.linkedRoomId() != null ? UUID.fromString(request.linkedRoomId()) : null;
        var message = messagingService.sendMessage(conversationId, userId, request.content(), replyTo, null, null, fileId, roomId, request.linkedFileName());
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

    @PostMapping("/conversations/{conversationId}/mute")
    public ResponseEntity<ApiResponse<Void>> muteConversation(@PathVariable UUID conversationId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        messagingService.muteConversation(conversationId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/conversations/{conversationId}/unmute")
    public ResponseEntity<ApiResponse<Void>> unmuteConversation(@PathVariable UUID conversationId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        messagingService.unmuteConversation(conversationId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        long count = messagingService.getTotalUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", count)));
    }

    @GetMapping("/images/{imageId}")
    public ResponseEntity<byte[]> getImage(@PathVariable UUID imageId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var image = messagingService.getImageEntity(imageId);
        try (var stream = messagingService.getImageForDownload(imageId, userId, false)) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(image.getContentType()))
                    .header("Cache-Control", "private, max-age=86400")
                    .body(stream.readAllBytes());
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read image", e);
        }
    }

    @GetMapping("/images/{imageId}/thumbnail")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable UUID imageId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        try (var stream = messagingService.getImageForDownload(imageId, userId, true)) {
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .header("Cache-Control", "private, max-age=86400")
                    .body(stream.readAllBytes());
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read thumbnail", e);
        }
    }

    @GetMapping("/attachments/{attachmentId}")
    public ResponseEntity<byte[]> getAttachment(@PathVariable UUID attachmentId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var att = messagingService.getAttachmentEntity(attachmentId);
        try (var stream = messagingService.getAttachmentForDownload(attachmentId, userId)) {
            String contentType = att.getContentType() != null ? att.getContentType() : "application/octet-stream";
            String filename = att.getOriginalFilename() != null ? att.getOriginalFilename() : "file";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .header("Cache-Control", "private, max-age=86400")
                    .body(stream.readAllBytes());
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read attachment", e);
        }
    }

    @PostMapping("/conversations/{conversationId}/polls")
    public ResponseEntity<ApiResponse<MessageInfo>> sendPollMessage(
            @PathVariable UUID conversationId,
            @RequestBody CreateMessagePollRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var message = messagingService.sendPollMessage(conversationId, userId, request);
        return ResponseEntity.ok(ApiResponse.ok(message));
    }

    @PostMapping("/messages/{messageId}/poll/vote")
    public ResponseEntity<ApiResponse<PollInfo>> voteMessagePoll(
            @PathVariable UUID messageId,
            @RequestBody Map<String, List<UUID>> request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var poll = messagingService.voteMessagePoll(messageId, userId, request.get("optionIds"));
        return ResponseEntity.ok(ApiResponse.ok(poll));
    }

    @PostMapping("/messages/{messageId}/poll/close")
    public ResponseEntity<ApiResponse<PollInfo>> closeMessagePoll(@PathVariable UUID messageId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var poll = messagingService.closeMessagePoll(messageId, userId);
        return ResponseEntity.ok(ApiResponse.ok(poll));
    }

    @PostMapping("/messages/{messageId}/reactions")
    public ResponseEntity<ApiResponse<java.util.List<MessageInfo.ReactionSummary>>> toggleMessageReaction(
            @PathVariable UUID messageId,
            @RequestBody Map<String, String> request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        String emoji = request.get("emoji");
        messagingService.toggleMessageReaction(messageId, userId, emoji);
        var reactions = messagingService.getMessageReactions(messageId, userId);
        return ResponseEntity.ok(ApiResponse.ok(reactions));
    }

    public record StartConversationRequest(
            String title,
            boolean isGroup,
            List<UUID> participantIds
    ) {
    }

    public record SendMessageRequest(String content, String replyToId, String linkedFileId, String linkedRoomId, String linkedFileName) {
    }
}
