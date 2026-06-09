package com.monteweb.notification.internal.service;

import com.monteweb.family.FamilyInvitationEvent;
import com.monteweb.notification.NotificationModuleApi;
import com.monteweb.notification.NotificationType;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class FamilyInvitationNotificationListener {

    private final NotificationModuleApi notificationModule;

    public FamilyInvitationNotificationListener(NotificationModuleApi notificationModule) {
        this.notificationModule = notificationModule;
    }

    @ApplicationModuleListener
    public void onFamilyInvitation(FamilyInvitationEvent event) {
        if (event.accepted()) {
            // Notify inviter that invitation was accepted
            notificationModule.sendNotification(
                    event.inviterId(),
                    NotificationType.FAMILY_INVITATION_ACCEPTED,
                    "Einladung angenommen",
                    "Ihre Familieneinladung für " + event.familyName() + " wurde angenommen",
                    "/family",
                    "FAMILY_INVITATION",
                    event.invitationId()
            );
        } else {
            // Notify invitee about new invitation
            notificationModule.sendNotification(
                    event.inviteeId(),
                    NotificationType.FAMILY_INVITATION,
                    "Familieneinladung",
                    event.inviterName() + " hat Sie zur Familie " + event.familyName() + " eingeladen",
                    "/family",
                    "FAMILY_INVITATION",
                    event.invitationId()
            );
        }
    }
}
