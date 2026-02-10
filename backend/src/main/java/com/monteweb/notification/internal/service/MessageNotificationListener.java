package com.monteweb.notification.internal.service;

import com.monteweb.messaging.MessageSentEvent;
import com.monteweb.notification.NotificationType;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class MessageNotificationListener {

    private final NotificationService notificationService;

    public MessageNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @ApplicationModuleListener
    public void onMessageSent(MessageSentEvent event) {
        String title = "Neue Nachricht von " + event.senderName();
        String link = "/messages/" + event.conversationId();

        for (var recipientId : event.recipientIds()) {
            notificationService.sendNotification(
                    recipientId,
                    NotificationType.MESSAGE,
                    title,
                    event.contentPreview(),
                    link,
                    "conversation",
                    event.conversationId()
            );
        }
    }
}
