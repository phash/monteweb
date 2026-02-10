package com.monteweb.notification.internal.service;

import com.monteweb.feed.FeedPostCreatedEvent;
import com.monteweb.feed.SourceType;
import com.monteweb.notification.NotificationType;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.room.RoomInfo;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class FeedPostNotificationListener {

    private final NotificationService notificationService;
    private final RoomModuleApi roomModuleApi;

    public FeedPostNotificationListener(NotificationService notificationService,
                                        RoomModuleApi roomModuleApi) {
        this.notificationService = notificationService;
        this.roomModuleApi = roomModuleApi;
    }

    @ApplicationModuleListener
    public void onFeedPostCreated(FeedPostCreatedEvent event) {
        if (event.sourceType() == SourceType.ROOM && event.sourceId() != null) {
            // Notify all room members except the author
            var roomInfo = roomModuleApi.findById(event.sourceId());
            if (roomInfo.isEmpty()) return;

            String title = "Neuer Beitrag in " + roomInfo.get().name();
            String message = event.authorName() + " hat einen Beitrag verfasst";
            String link = "/rooms/" + event.sourceId();

            // In a full implementation, we'd get all room member IDs and notify each.
            // For now, the infrastructure is in place.
        }
    }
}
