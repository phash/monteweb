package com.monteweb.notification;

import java.util.UUID;

/**
 * Public API: Facade interface for the notification module.
 * Other modules can send notifications through this interface.
 */
public interface NotificationModuleApi {

    void sendNotification(UUID userId, NotificationType type, String title, String message,
                          String link, String referenceType, UUID referenceId);

    long getUnreadCount(UUID userId);
}
