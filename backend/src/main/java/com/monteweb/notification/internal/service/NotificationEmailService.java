package com.monteweb.notification.internal.service;

import com.monteweb.notification.NotificationType;
import com.monteweb.shared.config.EmailProperties;
import com.monteweb.shared.config.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Sends an immediate email notification when a new in-app notification is created.
 * The email contains minimal information: WHAT happened and WHERE (room/section),
 * but deliberately omits WHO triggered it (privacy by design).
 */
@Service
@ConditionalOnProperty(name = "monteweb.email.enabled", havingValue = "true")
public class NotificationEmailService {

    private static final Logger log = LoggerFactory.getLogger(NotificationEmailService.class);

    private final EmailService emailService;
    private final EmailProperties emailProperties;

    public NotificationEmailService(EmailService emailService, EmailProperties emailProperties) {
        this.emailService = emailService;
        this.emailProperties = emailProperties;
    }

    public void sendNotificationEmail(String toEmail, String firstName, NotificationType type,
                                      String title, String link) {
        try {
            String description = getNotificationDescription(type, title);
            String subject = "MonteWeb - " + description;
            // Keep subject line reasonable length
            if (subject.length() > 100) {
                subject = subject.substring(0, 97) + "...";
            }
            String body = buildEmailBody(firstName, description, link);
            emailService.sendGenericEmail(toEmail, subject, body);
        } catch (Exception e) {
            log.error("Failed to send notification email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildEmailBody(String firstName, String description, String link) {
        StringBuilder body = new StringBuilder();
        body.append("Hallo ").append(firstName).append(",\n\n");
        body.append(description).append("\n\n");

        if (link != null) {
            body.append("Hier ansehen:\n");
            body.append(emailProperties.baseUrl()).append(link).append("\n\n");
        }

        body.append("---\n");
        body.append("Sie erhalten diese E-Mail, weil eine neue Aktivitaet in MonteWeb stattgefunden hat.\n");
        body.append(emailProperties.baseUrl()).append("/notifications\n\n");
        body.append("Mit freundlichen Gruessen,\nIhr MonteWeb-Team");
        return body.toString();
    }

    private String getNotificationDescription(NotificationType type, String title) {
        // For types where the title already contains the WHERE context (room/section/event),
        // we use it directly. For types where the title includes WHO (person names),
        // we use a generic privacy-respecting description instead.
        return switch (type) {
            case POST -> title; // e.g. "Neuer Beitrag in Sonnengruppe"
            case COMMENT -> title; // e.g. "Neuer Kommentar in Sonnengruppe"
            case MESSAGE -> "Sie haben eine neue Nachricht.";
            case SYSTEM -> title;
            case REMINDER -> title;
            case CLEANING_COMPLETED -> title;
            case JOB_COMPLETED -> title;
            case DISCUSSION_THREAD -> title;
            case DISCUSSION_REPLY -> title;
            case EVENT_CREATED -> title; // e.g. "Neuer Termin: Elternabend"
            case EVENT_UPDATED -> title;
            case EVENT_CANCELLED -> title;
            case FORM_PUBLISHED -> title;
            case CONSENT_REQUIRED -> title;
            case ROOM_JOIN_REQUEST -> "Neue Beitrittsanfrage fuer einen Ihrer Raeume.";
            case ROOM_JOIN_APPROVED -> "Ihre Beitrittsanfrage wurde genehmigt.";
            case ROOM_JOIN_DENIED -> "Ihre Beitrittsanfrage wurde abgelehnt.";
            case FAMILY_INVITATION -> "Sie haben eine neue Familieneinladung erhalten.";
            case FAMILY_INVITATION_ACCEPTED -> "Ihre Familieneinladung wurde angenommen.";
            case MENTION -> "Sie wurden in einem Beitrag erwaehnt.";
        };
    }
}
