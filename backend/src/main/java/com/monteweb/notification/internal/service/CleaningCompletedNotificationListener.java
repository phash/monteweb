package com.monteweb.notification.internal.service;

import com.monteweb.cleaning.CleaningCompletedEvent;
import com.monteweb.notification.NotificationType;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class CleaningCompletedNotificationListener {

    private final NotificationService notificationService;

    public CleaningCompletedNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @ApplicationModuleListener
    public void onCleaningCompleted(CleaningCompletedEvent event) {
        String title = "Putzstunden gutgeschrieben";
        String message = String.format(
                "Dir wurden %s Stunden fuer den Putzdienst gutgeschrieben (%d Minuten).",
                event.hoursCredit().toPlainString(), event.actualMinutes());

        notificationService.sendNotification(
                event.userId(),
                NotificationType.CLEANING_COMPLETED,
                title,
                message,
                "/cleaning/mine",
                "cleaning",
                null
        );
    }
}
