package com.monteweb.notification.internal.service;

import com.monteweb.notification.NotificationModuleApi;
import com.monteweb.notification.NotificationType;
import com.monteweb.room.RoomJoinRequestEvent;
import com.monteweb.room.RoomJoinRequestResolvedEvent;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.room.RoomRole;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class RoomJoinRequestNotificationListener {

    private final NotificationModuleApi notificationModule;
    private final RoomModuleApi roomModule;

    public RoomJoinRequestNotificationListener(NotificationModuleApi notificationModule,
                                                RoomModuleApi roomModule) {
        this.notificationModule = notificationModule;
        this.roomModule = roomModule;
    }

    @Async
    @EventListener
    public void onRoomJoinRequest(RoomJoinRequestEvent event) {
        String title = "Beitrittsanfrage: " + event.roomName();
        String message = event.requesterName() + " möchte dem Raum " + event.roomName() + " beitreten";
        String link = "/rooms/" + event.roomId();

        var memberIds = roomModule.getMemberUserIds(event.roomId());
        for (var memberId : memberIds) {
            var role = roomModule.getUserRoleInRoom(memberId, event.roomId());
            if (role.isPresent() && role.get() == RoomRole.LEADER) {
                notificationModule.sendNotification(
                        memberId,
                        NotificationType.ROOM_JOIN_REQUEST,
                        title,
                        message,
                        link,
                        "ROOM_JOIN_REQUEST",
                        event.requestId()
                );
            }
        }
    }

    @Async
    @EventListener
    public void onRoomJoinRequestResolved(RoomJoinRequestResolvedEvent event) {
        String title;
        String message;
        NotificationType type;

        if (event.approved()) {
            title = "Beitrittsanfrage angenommen";
            message = "Ihre Anfrage für " + event.roomName() + " wurde angenommen";
            type = NotificationType.ROOM_JOIN_APPROVED;
        } else {
            title = "Beitrittsanfrage abgelehnt";
            message = "Ihre Anfrage für " + event.roomName() + " wurde abgelehnt";
            type = NotificationType.ROOM_JOIN_DENIED;
        }

        notificationModule.sendNotification(
                event.requesterId(),
                type,
                title,
                message,
                "/rooms/" + event.roomId(),
                "ROOM_JOIN_REQUEST",
                event.requestId()
        );
    }
}
