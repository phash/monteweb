package com.monteweb.notification.internal.service;

import com.monteweb.messaging.MessageSentEvent;
import com.monteweb.messaging.MessagingModuleApi;
import com.monteweb.notification.NotificationType;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class MessageNotificationListener {

    private final NotificationService notificationService;
    private final MessagingModuleApi messagingModuleApi;

    public MessageNotificationListener(NotificationService notificationService,
                                       MessagingModuleApi messagingModuleApi) {
        this.notificationService = notificationService;
        this.messagingModuleApi = messagingModuleApi;
    }

    @ApplicationModuleListener
    public void onMessageSent(MessageSentEvent event) {
        String title = "Neue Nachricht von " + event.senderName();
        String link = "/messages/" + event.conversationId();

        for (var recipientId : event.recipientIds()) {
            // Skip notification if the recipient has muted this conversation
            if (messagingModuleApi.isConversationMutedByUser(event.conversationId(), recipientId)) {
                continue;
            }

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
