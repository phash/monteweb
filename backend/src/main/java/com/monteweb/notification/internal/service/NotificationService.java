package com.monteweb.notification.internal.service;

import com.monteweb.notification.NotificationInfo;
import com.monteweb.notification.NotificationModuleApi;
import com.monteweb.notification.NotificationType;
import com.monteweb.notification.internal.model.Notification;
import com.monteweb.notification.internal.repository.NotificationRepository;
import com.monteweb.notification.internal.repository.PushSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class NotificationService implements NotificationModuleApi {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final Optional<WebPushService> webPushService;

    public NotificationService(NotificationRepository repository,
                               PushSubscriptionRepository pushSubscriptionRepository,
                               SimpMessagingTemplate messagingTemplate,
                               Optional<WebPushService> webPushService) {
        this.repository = repository;
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.messagingTemplate = messagingTemplate;
        this.webPushService = webPushService;
    }

    @Override
    @Transactional
    public void sendNotification(UUID userId, NotificationType type, String title, String message,
                                 String link, String referenceType, UUID referenceId) {
        var notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLink(link);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);

        notification = repository.save(notification);

        // Push via WebSocket to the specific user
        var info = toInfo(notification);
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                info
        );

        // Also send Web Push for offline users
        webPushService.ifPresent(push ->
                push.sendPushToUser(userId, title, message, link));
    }

    @Override
    public long getUnreadCount(UUID userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public int deleteOlderThan(java.time.Instant cutoff) {
        return repository.deleteByCreatedAtBefore(cutoff);
    }

    public Page<NotificationInfo> findByUser(UUID userId, Pageable pageable) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toInfo);
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        repository.markAsRead(notificationId, userId);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        repository.markAllAsRead(userId);
    }

    @Transactional
    public void deleteNotification(UUID notificationId, UUID userId) {
        repository.deleteByIdAndUserId(notificationId, userId);
    }

    public List<NotificationInfo> findUnreadSince(UUID userId, Instant since) {
        return repository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(userId, since)
                .stream()
                .map(this::toInfo)
                .toList();
    }

    @Override
    public Map<String, Object> exportUserData(UUID userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("notifications", repository.findByUserId(userId));
        data.put("pushSubscriptions", pushSubscriptionRepository.findByUserId(userId));
        return data;
    }

    @Transactional
    public void cleanupUserData(UUID userId) {
        repository.deleteByUserId(userId);
        pushSubscriptionRepository.deleteByUserId(userId);
        log.info("Cleaned up notifications and push subscriptions for deleted user {}", userId);
    }

    private NotificationInfo toInfo(Notification n) {
        return new NotificationInfo(
                n.getId(), n.getUserId(), n.getType(), n.getTitle(), n.getMessage(),
                n.getLink(), n.getReferenceType(), n.getReferenceId(), n.isRead(), n.getCreatedAt()
        );
    }
}
