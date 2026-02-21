package com.monteweb.messaging.internal.service;

import com.monteweb.admin.AdminModuleApi;
import com.monteweb.messaging.ConversationInfo;
import com.monteweb.messaging.MessageInfo;
import com.monteweb.messaging.MessageSentEvent;
import com.monteweb.messaging.MessagingModuleApi;
import com.monteweb.messaging.internal.model.Conversation;
import com.monteweb.messaging.internal.model.ConversationParticipant;
import com.monteweb.messaging.internal.model.Message;
import com.monteweb.messaging.internal.model.MessageImage;
import com.monteweb.messaging.internal.repository.ConversationParticipantRepository;
import com.monteweb.messaging.internal.repository.ConversationRepository;
import com.monteweb.messaging.internal.repository.MessageImageRepository;
import com.monteweb.messaging.internal.repository.MessageRepository;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.exception.ForbiddenException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@ConditionalOnProperty(prefix = "monteweb.modules.messaging", name = "enabled", havingValue = "true")
public class MessagingService implements MessagingModuleApi {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final MessageImageRepository messageImageRepository;
    private final UserModuleApi userModuleApi;
    private final AdminModuleApi adminModuleApi;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final MessageStorageService storageService;

    public MessagingService(ConversationRepository conversationRepository,
                            ConversationParticipantRepository participantRepository,
                            MessageRepository messageRepository,
                            MessageImageRepository messageImageRepository,
                            UserModuleApi userModuleApi,
                            AdminModuleApi adminModuleApi,
                            SimpMessagingTemplate messagingTemplate,
                            ApplicationEventPublisher eventPublisher,
                            MessageStorageService storageService) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.messageImageRepository = messageImageRepository;
        this.userModuleApi = userModuleApi;
        this.adminModuleApi = adminModuleApi;
        this.messagingTemplate = messagingTemplate;
        this.eventPublisher = eventPublisher;
        this.storageService = storageService;
    }

    // ---- Public API (MessagingModuleApi) ----

    @Override
    @Transactional(readOnly = true)
    public Optional<ConversationInfo> findConversationById(UUID conversationId, UUID userId) {
        return conversationRepository.findById(conversationId)
                .filter(c -> participantRepository.existsByConversationIdAndUserId(conversationId, userId))
                .map(c -> toConversationInfo(c, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationInfo> findConversationsByUser(UUID userId) {
        return conversationRepository.findByParticipantUserId(userId).stream()
                .map(c -> toConversationInfo(c, userId))
                .toList();
    }

    @Override
    public ConversationInfo createGroupConversation(String title, UUID creatorId, List<UUID> memberIds) {
        return startGroupConversation(creatorId, title, memberIds);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalUnreadCount(UUID userId) {
        var participants = participantRepository.findByUserId(userId);
        long total = 0;
        for (var p : participants) {
            Instant since = p.getLastReadAt() != null ? p.getLastReadAt() : p.getJoinedAt();
            total += messageRepository.countUnreadMessages(p.getConversationId(), userId, since);
        }
        return total;
    }

    // ---- Conversation operations ----

    public ConversationInfo startDirectConversation(UUID userId, UUID otherUserId) {
        if (userId.equals(otherUserId)) {
            throw new BusinessException("Cannot start a conversation with yourself");
        }

        // Enforce communication rules
        enforceCommRules(userId, otherUserId);

        // Check if direct conversation already exists
        var existing = conversationRepository.findDirectConversation(userId, otherUserId);
        if (!existing.isEmpty()) {
            return toConversationInfo(existing.get(0), userId);
        }

        var conversation = new Conversation();
        conversation.setGroup(false);
        conversation.setCreatedBy(userId);
        conversation = conversationRepository.save(conversation);

        addParticipant(conversation.getId(), userId);
        addParticipant(conversation.getId(), otherUserId);

        return toConversationInfo(conversation, userId);
    }

    public ConversationInfo startGroupConversation(UUID userId, String title, List<UUID> participantIds) {
        var conversation = new Conversation();
        conversation.setTitle(title);
        conversation.setGroup(true);
        conversation.setCreatedBy(userId);
        conversation = conversationRepository.save(conversation);

        addParticipant(conversation.getId(), userId);
        for (UUID participantId : participantIds) {
            if (!participantId.equals(userId)) {
                addParticipant(conversation.getId(), participantId);
            }
        }

        return toConversationInfo(conversation, userId);
    }

    // ---- Message operations ----

    @Transactional(readOnly = true)
    public Page<MessageInfo> getMessages(UUID conversationId, UUID userId, Pageable pageable) {
        requireParticipant(conversationId, userId);
        var page = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);

        // Batch-load images and reply data to avoid N+1
        var messageIds = page.getContent().stream().map(Message::getId).toList();
        var allImages = messageImageRepository.findByMessageIdIn(messageIds);
        var imagesByMessageId = allImages.stream()
                .collect(java.util.stream.Collectors.groupingBy(MessageImage::getMessageId));

        // Collect reply-to IDs
        var replyToIds = page.getContent().stream()
                .map(Message::getReplyToId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        var replyMessages = replyToIds.isEmpty()
                ? Map.<UUID, Message>of()
                : messageRepository.findAllById(replyToIds).stream()
                    .collect(java.util.stream.Collectors.toMap(Message::getId, m -> m));
        // Images for replied-to messages
        var replyImagesByMsgId = replyToIds.isEmpty()
                ? Map.<UUID, List<MessageImage>>of()
                : messageImageRepository.findByMessageIdIn(replyToIds).stream()
                    .collect(java.util.stream.Collectors.groupingBy(MessageImage::getMessageId));

        return page.map(m -> toMessageInfo(m,
                imagesByMessageId.getOrDefault(m.getId(), List.of()),
                replyMessages, replyImagesByMsgId));
    }

    public MessageInfo sendMessage(UUID conversationId, UUID senderId, String content,
                                    UUID replyToId, MultipartFile image) {
        requireParticipant(conversationId, senderId);

        // Validate: must have content or image
        boolean hasContent = content != null && !content.isBlank();
        boolean hasImage = image != null && !image.isEmpty();
        if (!hasContent && !hasImage) {
            throw new BusinessException("Message must have text content or an image");
        }

        // Validate replyToId
        if (replyToId != null && !messageRepository.existsById(replyToId)) {
            throw new BusinessException("Reply-to message not found");
        }

        var message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setContent(hasContent ? content : null);
        message.setReplyToId(replyToId);
        message = messageRepository.save(message);

        // Handle image upload
        List<MessageImage> images = List.of();
        if (hasImage) {
            // Validate file size (10MB)
            if (image.getSize() > 10 * 1024 * 1024) {
                throw new BusinessException("Image file size exceeds 10MB limit");
            }
            String contentType = storageService.validateAndDetectContentType(image);
            String extension = MessageStorageService.extensionFromContentType(contentType);

            var imgEntity = new MessageImage();
            imgEntity.setMessageId(message.getId());
            imgEntity.setUploadedBy(senderId);
            imgEntity.setOriginalFilename(image.getOriginalFilename() != null ? image.getOriginalFilename() : "image." + extension);
            imgEntity.setFileSize(image.getSize());
            imgEntity.setContentType(contentType);

            // Pre-generate ID for storage path
            imgEntity = messageImageRepository.save(imgEntity);

            String storagePath = storageService.uploadOriginal(conversationId, imgEntity.getId(), extension, image, contentType);
            String thumbPath = storageService.uploadThumbnail(conversationId, imgEntity.getId(), extension, image);

            imgEntity.setStoragePath(storagePath);
            imgEntity.setThumbnailPath(thumbPath);
            messageImageRepository.save(imgEntity);

            images = List.of(imgEntity);
        }

        // Update conversation timestamp
        conversationRepository.findById(conversationId).ifPresent(c -> {
            c.setUpdatedAt(Instant.now());
            conversationRepository.save(c);
        });

        // Mark as read for sender
        participantRepository.markAsRead(conversationId, senderId, Instant.now());

        var messageInfo = toMessageInfo(message, images, Map.of(), Map.of());

        // Push via WebSocket to all participants
        var participants = participantRepository.findByConversationId(conversationId);
        for (var p : participants) {
            messagingTemplate.convertAndSendToUser(
                    p.getUserId().toString(),
                    "/queue/messages",
                    messageInfo);
        }

        // Publish event for notification module
        var recipientIds = participants.stream()
                .map(ConversationParticipant::getUserId)
                .filter(id -> !id.equals(senderId))
                .toList();

        String senderName = userModuleApi.findById(senderId)
                .map(u -> u.firstName() + " " + u.lastName())
                .orElse("Unknown");

        String preview;
        if (hasContent) {
            preview = content.length() > 100 ? content.substring(0, 100) + "..." : content;
        } else {
            preview = "\uD83D\uDDBC Bild";
        }

        eventPublisher.publishEvent(new MessageSentEvent(
                message.getId(),
                conversationId,
                senderId,
                senderName,
                preview,
                recipientIds,
                hasImage
        ));

        return messageInfo;
    }

    /**
     * Get image for download — verifies the requester is a participant.
     */
    @Transactional(readOnly = true)
    public java.io.InputStream getImageForDownload(UUID imageId, UUID userId, boolean thumbnail) {
        var image = messageImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("MessageImage", imageId));

        // Check that user is a participant of the conversation containing this message
        var message = messageRepository.findById(image.getMessageId())
                .orElseThrow(() -> new ResourceNotFoundException("Message", image.getMessageId()));
        requireParticipant(message.getConversationId(), userId);

        String path = thumbnail && image.getThumbnailPath() != null
                ? image.getThumbnailPath()
                : image.getStoragePath();
        return storageService.download(path);
    }

    @Transactional(readOnly = true)
    public MessageImage getImageEntity(UUID imageId) {
        return messageImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("MessageImage", imageId));
    }

    public void markConversationAsRead(UUID conversationId, UUID userId) {
        requireParticipant(conversationId, userId);
        participantRepository.markAsRead(conversationId, userId, Instant.now());
    }

    public void deleteConversation(UUID conversationId, UUID userId) {
        requireParticipant(conversationId, userId);
        participantRepository.deleteByConversationIdAndUserId(conversationId, userId);
        // If no participants remain, delete the conversation entirely
        if (participantRepository.countByConversationId(conversationId) == 0) {
            conversationRepository.deleteById(conversationId);
        }
    }


    @Transactional
    public void muteConversation(UUID conversationId, UUID userId) {
        requireParticipant(conversationId, userId);
        var id = new ConversationParticipant.ConversationParticipantId();
        id.setConversationId(conversationId);
        id.setUserId(userId);
        var participant = participantRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Not a participant"));
        participant.setMuted(true);
        participantRepository.save(participant);
    }

    @Transactional
    public void unmuteConversation(UUID conversationId, UUID userId) {
        requireParticipant(conversationId, userId);
        var id = new ConversationParticipant.ConversationParticipantId();
        id.setConversationId(conversationId);
        id.setUserId(userId);
        var participant = participantRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Not a participant"));
        participant.setMuted(false);
        participantRepository.save(participant);
    }

    @Transactional(readOnly = true)
    public boolean isConversationMutedByUser(UUID conversationId, UUID userId) {
        return participantRepository.findMutedConversationIdsByUserId(userId).contains(conversationId);
    }

    // ---- Communication Rules ----

    private void enforceCommRules(UUID userId, UUID otherUserId) {
        var user = userModuleApi.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        var other = userModuleApi.findById(otherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", otherUserId));

        // Teacher/Admin can always message anyone
        if (isStaffRole(user.role()) || isStaffRole(other.role())) {
            return;
        }

        var config = adminModuleApi.getTenantConfig();

        // Parent-to-Parent
        if (user.role() == UserRole.PARENT && other.role() == UserRole.PARENT) {
            if (!config.parentToParentMessaging()) {
                throw new ForbiddenException("Parent-to-parent messaging is not enabled");
            }
            return;
        }

        // Student-to-Student
        if (user.role() == UserRole.STUDENT && other.role() == UserRole.STUDENT) {
            if (!config.studentToStudentMessaging()) {
                throw new ForbiddenException("Student-to-student messaging is not enabled");
            }
            return;
        }

        // Other combinations (parent-student etc.) are not allowed by default
        throw new ForbiddenException("Messaging between these user roles is not allowed");
    }

    private boolean isStaffRole(UserRole role) {
        return role == UserRole.SUPERADMIN || role == UserRole.SECTION_ADMIN || role == UserRole.TEACHER;
    }

    // ---- Public API: participant management ----

    @Override
    public void addParticipantToConversation(UUID conversationId, UUID userId) {
        if (!participantRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            addParticipant(conversationId, userId);
        }
    }

    @Override
    public void removeParticipantFromConversation(UUID conversationId, UUID userId) {
        participantRepository.deleteByConversationIdAndUserId(conversationId, userId);
    }

    // ---- Helpers ----

    private void addParticipant(UUID conversationId, UUID userId) {
        var participant = new ConversationParticipant();
        participant.setConversationId(conversationId);
        participant.setUserId(userId);
        participantRepository.save(participant);
    }

    private void requireParticipant(UUID conversationId, UUID userId) {
        if (!participantRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new ForbiddenException("You are not a participant of this conversation");
        }
    }

    private ConversationInfo toConversationInfo(Conversation c, UUID currentUserId) {
        var participants = participantRepository.findByConversationId(c.getId()).stream()
                .map(p -> {
                    String name = userModuleApi.findById(p.getUserId())
                            .map(u -> u.firstName() + " " + u.lastName())
                            .orElse("Unknown");
                    return new ConversationInfo.ParticipantInfo(p.getUserId(), name, p.getLastReadAt());
                })
                .toList();

        var lastMsg = messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(c.getId());
        String lastMessage = lastMsg.map(m -> {
            if (m.getContent() != null) return m.getContent();
            // Image-only message
            return "\uD83D\uDDBC Bild";
        }).orElse(null);
        Instant lastMessageAt = lastMsg.map(Message::getCreatedAt).orElse(null);

        // Calculate unread for current user
        var currentParticipant = participantRepository.findByConversationId(c.getId()).stream()
                .filter(p -> p.getUserId().equals(currentUserId))
                .findFirst();
        long unread = 0;
        boolean muted = false;
        if (currentParticipant.isPresent()) {
            Instant since = currentParticipant.get().getLastReadAt() != null
                    ? currentParticipant.get().getLastReadAt()
                    : currentParticipant.get().getJoinedAt();
            unread = messageRepository.countUnreadMessages(c.getId(), currentUserId, since);
            muted = currentParticipant.get().isMuted();
        }

        return new ConversationInfo(
                c.getId(),
                c.getTitle(),
                c.isGroup(),
                participants,
                lastMessage,
                lastMessageAt,
                unread,
                muted,
                c.getCreatedAt()
        );
    }

    private MessageInfo toMessageInfo(Message m, List<MessageImage> images,
                                       Map<UUID, Message> replyMessages,
                                       Map<UUID, List<MessageImage>> replyImagesByMsgId) {
        String senderName = userModuleApi.findById(m.getSenderId())
                .map(u -> u.firstName() + " " + u.lastName())
                .orElse("Unknown");

        List<MessageInfo.MessageImageInfo> imageInfos = images.stream()
                .map(img -> new MessageInfo.MessageImageInfo(
                        img.getId(), img.getOriginalFilename(), img.getContentType(), img.getFileSize()))
                .toList();

        MessageInfo.ReplyInfo replyInfo = null;
        if (m.getReplyToId() != null) {
            var replyMsg = replyMessages.get(m.getReplyToId());
            if (replyMsg != null) {
                String replySenderName = userModuleApi.findById(replyMsg.getSenderId())
                        .map(u -> u.firstName() + " " + u.lastName())
                        .orElse("Unknown");
                String contentPreview = replyMsg.getContent() != null
                        ? (replyMsg.getContent().length() > 80 ? replyMsg.getContent().substring(0, 80) + "..." : replyMsg.getContent())
                        : null;
                boolean replyHasImage = replyImagesByMsgId.containsKey(replyMsg.getId())
                        && !replyImagesByMsgId.get(replyMsg.getId()).isEmpty();
                replyInfo = new MessageInfo.ReplyInfo(
                        replyMsg.getId(), replyMsg.getSenderId(), replySenderName, contentPreview, replyHasImage);
            }
        }

        return new MessageInfo(
                m.getId(),
                m.getConversationId(),
                m.getSenderId(),
                senderName,
                m.getContent(),
                m.getCreatedAt(),
                imageInfos,
                replyInfo
        );
    }
}
