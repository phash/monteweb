package com.monteweb.notification.internal.service;

import com.monteweb.jobboard.JobCompletedEvent;
import com.monteweb.notification.NotificationType;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class JobCompletedNotificationListener {

    private final NotificationService notificationService;

    public JobCompletedNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @ApplicationModuleListener
    public void onJobCompleted(JobCompletedEvent event) {
        String title = "Elternstunden bestätigt";
        String message = event.hours() + " Stunden für \"" + event.jobTitle() + "\" wurden bestätigt.";
        String link = "/jobs/" + event.jobId();

        notificationService.sendNotification(
                event.userId(),
                NotificationType.JOB_COMPLETED,
                title,
                message,
                link,
                "job",
                event.jobId()
        );
    }
}
