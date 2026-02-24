package com.monteweb.notification.internal.service;

import com.monteweb.notification.NotificationInfo;
import com.monteweb.shared.config.EmailProperties;
import com.monteweb.shared.config.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "monteweb.email.enabled", havingValue = "true")
public class DigestEmailService {

    private static final Logger log = LoggerFactory.getLogger(DigestEmailService.class);

    private final EmailService emailService;
    private final EmailProperties emailProperties;

    public DigestEmailService(EmailService emailService, EmailProperties emailProperties) {
        this.emailService = emailService;
        this.emailProperties = emailProperties;
    }

    public void sendDigest(String toEmail, String firstName, List<NotificationInfo> notifications) {
        if (notifications.isEmpty()) return;

        StringBuilder body = new StringBuilder();
        body.append("Hallo ").append(firstName).append(",\n\n");
        body.append("Hier ist Ihre Zusammenfassung von MonteWeb:\n\n");

        for (NotificationInfo n : notifications) {
            body.append("\u2022 ").append(n.title());
            if (n.message() != null && !n.message().isBlank()) {
                body.append(": ").append(n.message());
            }
            if (n.link() != null) {
                body.append("\n  \u2192 ").append(emailProperties.baseUrl()).append(n.link());
            }
            body.append("\n\n");
        }

        body.append("---\n");
        body.append("Sie koennen die Haeufigkeit der Zusammenfassung in Ihrem Profil aendern:\n");
        body.append(emailProperties.baseUrl()).append("/profile\n\n");
        body.append("Mit freundlichen Gruessen,\nIhr MonteWeb-Team");

        emailService.sendGenericEmail(toEmail, "MonteWeb - Ihre Zusammenfassung", body.toString());
    }
}
