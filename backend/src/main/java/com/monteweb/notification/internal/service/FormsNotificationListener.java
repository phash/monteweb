package com.monteweb.notification.internal.service;

import com.monteweb.forms.FormPublishedEvent;
import com.monteweb.forms.FormScope;
import com.monteweb.forms.FormType;
import com.monteweb.notification.NotificationModuleApi;
import com.monteweb.notification.NotificationType;
import com.monteweb.room.RoomModuleApi;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class FormsNotificationListener {

    private final NotificationModuleApi notificationModule;
    private final RoomModuleApi roomModule;

    public FormsNotificationListener(NotificationModuleApi notificationModule,
                                     RoomModuleApi roomModule) {
        this.notificationModule = notificationModule;
        this.roomModule = roomModule;
    }

    @Async
    @EventListener
    public void onFormPublished(FormPublishedEvent event) {
        if (event.scope() != FormScope.ROOM || event.scopeId() == null) return;

        NotificationType type = event.type() == FormType.CONSENT
                ? NotificationType.CONSENT_REQUIRED
                : NotificationType.FORM_PUBLISHED;

        String title = event.type() == FormType.CONSENT
                ? "Einverstaendniserklaerung: " + event.title()
                : "Neue Umfrage: " + event.title();
        String message = event.publisherName() + " hat ein neues Formular veroeffentlicht";
        String link = "/forms/" + event.formId();

        List<UUID> memberIds = roomModule.getMemberUserIds(event.scopeId());
        for (var memberId : memberIds) {
            if (!memberId.equals(event.publishedBy())) {
                notificationModule.sendNotification(
                        memberId, type, title, message, link,
                        "FORM", event.formId()
                );
            }
        }
    }
}
