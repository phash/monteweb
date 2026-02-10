package com.monteweb.notification.internal.service;

import com.monteweb.cleaning.CleaningCompletedEvent;
import com.monteweb.notification.internal.model.Notification;
import com.monteweb.notification.internal.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CleaningCompletedNotificationListener {

    private final NotificationRepository notificationRepository;

    @ApplicationModuleListener
    public void onCleaningCompleted(CleaningCompletedEvent event) {
        Notification notification = new Notification();
        notification.setUserId(event.userId());
        notification.setType("CLEANING_COMPLETED");
        notification.setTitle("Putzstunden gutgeschrieben");
        notification.setMessage(String.format(
                "Dir wurden %s Stunden für den Putzdienst gutgeschrieben (%d Minuten).",
                event.hoursCredit().toPlainString(), event.actualMinutes()));
        notification.setLink("/cleaning/mine");
        notificationRepository.save(notification);
    }
}
