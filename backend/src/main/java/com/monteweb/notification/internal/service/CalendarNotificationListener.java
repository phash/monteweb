package com.monteweb.notification.internal.service;

import com.monteweb.calendar.EventCancelledEvent;
import com.monteweb.calendar.EventCreatedEvent;
import com.monteweb.calendar.EventScope;
import com.monteweb.notification.NotificationModuleApi;
import com.monteweb.notification.NotificationType;
import com.monteweb.room.RoomModuleApi;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class CalendarNotificationListener {

    private final NotificationModuleApi notificationModule;
    private final RoomModuleApi roomModule;

    public CalendarNotificationListener(NotificationModuleApi notificationModule,
                                        RoomModuleApi roomModule) {
        this.notificationModule = notificationModule;
        this.roomModule = roomModule;
    }

    @Async
    @EventListener
    public void onEventCreated(EventCreatedEvent event) {
        if (event.scope() != EventScope.ROOM || event.scopeId() == null) return;

        String title = "Neuer Termin: " + event.eventTitle();
        String message = event.creatorName() + " hat einen neuen Termin erstellt";
        String link = "/calendar/events/" + event.eventId();

        notifyRoomMembers(event.scopeId(), event.createdBy(), NotificationType.EVENT_CREATED,
                title, message, link, event.eventId());
    }

    @Async
    @EventListener
    public void onEventCancelled(EventCancelledEvent event) {
        if (event.scope() != EventScope.ROOM || event.scopeId() == null) return;

        String title = "Termin abgesagt: " + event.eventTitle();
        String message = event.cancellerName() + " hat den Termin abgesagt";
        String link = "/calendar/events/" + event.eventId();

        notifyRoomMembers(event.scopeId(), event.cancelledBy(), NotificationType.EVENT_CANCELLED,
                title, message, link, event.eventId());
    }

    private void notifyRoomMembers(UUID roomId, UUID excludeUserId, NotificationType type,
                                    String title, String message, String link, UUID referenceId) {
        List<UUID> memberIds = roomModule.getMemberUserIds(roomId);
        for (var memberId : memberIds) {
            if (!memberId.equals(excludeUserId)) {
                notificationModule.sendNotification(
                        memberId, type, title, message, link,
                        "CALENDAR_EVENT", referenceId
                );
            }
        }
    }
}
