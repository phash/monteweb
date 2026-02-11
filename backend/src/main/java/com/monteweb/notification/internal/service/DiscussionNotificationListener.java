package com.monteweb.notification.internal.service;

import com.monteweb.notification.NotificationModuleApi;
import com.monteweb.notification.NotificationType;
import com.monteweb.room.DiscussionThreadCreatedEvent;
import com.monteweb.room.RoomModuleApi;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class DiscussionNotificationListener {

    private final NotificationModuleApi notificationModule;
    private final RoomModuleApi roomModule;

    public DiscussionNotificationListener(NotificationModuleApi notificationModule,
                                          RoomModuleApi roomModule) {
        this.notificationModule = notificationModule;
        this.roomModule = roomModule;
    }

    @Async
    @EventListener
    public void onDiscussionThreadCreated(DiscussionThreadCreatedEvent event) {
        var room = roomModule.findById(event.roomId()).orElse(null);
        if (room == null) return;

        String title = "Neue Diskussion: " + event.threadTitle();
        String message = event.creatorName() + " hat eine neue Diskussion in " + room.name() + " gestartet";
        String link = "/rooms/" + event.roomId();

        var memberIds = roomModule.getMemberUserIds(event.roomId());
        for (var memberId : memberIds) {
            if (!memberId.equals(event.createdBy())) {
                notificationModule.sendNotification(
                        memberId,
                        NotificationType.DISCUSSION_THREAD,
                        title,
                        message,
                        link,
                        "DISCUSSION_THREAD",
                        event.threadId()
                );
            }
        }
    }
}
