package com.monteweb.messaging.internal.service;

import com.monteweb.messaging.ConversationInfo;
import com.monteweb.messaging.MessageInfo;
import com.monteweb.messaging.MessageSentEvent;
import com.monteweb.messaging.MessagingModuleApi;
import com.monteweb.messaging.internal.model.Conversation;
import com.monteweb.messaging.internal.model.ConversationParticipant;
import com.monteweb.messaging.internal.model.Message;
import com.monteweb.messaging.internal.repository.ConversationParticipantRepository;
import com.monteweb.messaging.internal.repository.ConversationRepository;
import com.monteweb.messaging.internal.repository.MessageRepository;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.exception.ForbiddenException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import com.monteweb.user.UserModuleApi;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class MessagingService implements MessagingModuleApi {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final UserModuleApi userModuleApi;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public MessagingService(ConversationRepository conversationRepository,
                            ConversationParticipantRepository participantRepository,
                            MessageRepository messageRepository,
                            UserModuleApi userModuleApi,
                            SimpMessagingTemplate messagingTemplate,
                            ApplicationEventPublisher eventPublisher) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.userModuleApi = userModuleApi;
        this.messagingTemplate = messagingTemplate;
        this.eventPublisher = eventPublisher;
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
        return messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable)
                .map(this::toMessageInfo);
    }

    public MessageInfo sendMessage(UUID conversationId, UUID senderId, String content) {
        requireParticipant(conversationId, senderId);

        var message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setContent(content);
        message = messageRepository.save(message);

        // Update conversation timestamp
        conversationRepository.findById(conversationId).ifPresent(c -> {
            c.setUpdatedAt(Instant.now());
            conversationRepository.save(c);
        });

        // Mark as read for sender
        participantRepository.markAsRead(conversationId, senderId, Instant.now());

        var messageInfo = toMessageInfo(message);

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

        String preview = content.length() > 100 ? content.substring(0, 100) + "..." : content;

        eventPublisher.publishEvent(new MessageSentEvent(
                message.getId(),
                conversationId,
                senderId,
                senderName,
                preview,
                recipientIds
        ));

        return messageInfo;
    }

    public void markConversationAsRead(UUID conversationId, UUID userId) {
        requireParticipant(conversationId, userId);
        participantRepository.markAsRead(conversationId, userId, Instant.now());
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
        String lastMessage = lastMsg.map(Message::getContent).orElse(null);
        Instant lastMessageAt = lastMsg.map(Message::getCreatedAt).orElse(null);

        // Calculate unread for current user
        var currentParticipant = participantRepository.findByConversationId(c.getId()).stream()
                .filter(p -> p.getUserId().equals(currentUserId))
                .findFirst();
        long unread = 0;
        if (currentParticipant.isPresent()) {
            Instant since = currentParticipant.get().getLastReadAt() != null
                    ? currentParticipant.get().getLastReadAt()
                    : currentParticipant.get().getJoinedAt();
            unread = messageRepository.countUnreadMessages(c.getId(), currentUserId, since);
        }

        return new ConversationInfo(
                c.getId(),
                c.getTitle(),
                c.isGroup(),
                participants,
                lastMessage,
                lastMessageAt,
                unread,
                c.getCreatedAt()
        );
    }

    private MessageInfo toMessageInfo(Message m) {
        String senderName = userModuleApi.findById(m.getSenderId())
                .map(u -> u.firstName() + " " + u.lastName())
                .orElse("Unknown");

        return new MessageInfo(
                m.getId(),
                m.getConversationId(),
                m.getSenderId(),
                senderName,
                m.getContent(),
                m.getCreatedAt()
        );
    }
}
