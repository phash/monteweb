package com.monteweb.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "monteweb.email.enabled", havingValue = "true")
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final EmailProperties properties;

    public EmailService(JavaMailSender mailSender, EmailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetUrl = properties.baseUrl() + "/login?reset=" + resetToken;

        var message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(toEmail);
        message.setSubject("MonteWeb - Passwort zurücksetzen");
        message.setText(
                "Hallo,\n\n" +
                "Sie haben eine Passwort-Zurücksetzung angefordert.\n" +
                "Klicken Sie auf folgenden Link, um Ihr Passwort zu ändern:\n\n" +
                resetUrl + "\n\n" +
                "Der Link ist 24 Stunden gültig.\n\n" +
                "Falls Sie diese Anfrage nicht gestellt haben, können Sie diese E-Mail ignorieren.\n\n" +
                "Mit freundlichen Grüßen,\nIhr MonteWeb-Team"
        );

        try {
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    public void sendWelcomeEmail(String toEmail, String firstName) {
        var message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(toEmail);
        message.setSubject("Willkommen bei MonteWeb");
        message.setText(
                "Hallo " + firstName + ",\n\n" +
                "willkommen bei MonteWeb! Ihr Konto wurde erfolgreich erstellt.\n\n" +
                "Sie können sich jetzt unter folgendem Link anmelden:\n" +
                properties.baseUrl() + "/login\n\n" +
                "Mit freundlichen Grüßen,\nIhr MonteWeb-Team"
        );

        try {
            mailSender.send(message);
            log.info("Welcome email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }

    public void sendGenericEmail(String toEmail, String subject, String body) {
        var message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
            log.info("Email sent to {}: {}", toEmail, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }
}
