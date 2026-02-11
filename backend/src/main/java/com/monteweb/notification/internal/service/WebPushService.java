package com.monteweb.notification.internal.service;

import com.monteweb.notification.internal.model.PushSubscription;
import com.monteweb.notification.internal.repository.PushSubscriptionRepository;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "monteweb.push", name = "enabled", havingValue = "true")
public class WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);

    private final PushSubscriptionRepository subscriptionRepository;

    @Value("${monteweb.push.public-key}")
    private String publicKey;

    @Value("${monteweb.push.private-key}")
    private String privateKey;

    @Value("${monteweb.push.subject:mailto:admin@monteweb.local}")
    private String subject;

    private PushService pushService;

    public WebPushService(PushSubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @PostConstruct
    public void init() throws GeneralSecurityException {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        pushService = new PushService(publicKey, privateKey, subject);
    }

    public String getPublicKey() {
        return publicKey;
    }

    @Transactional
    public void subscribe(UUID userId, String endpoint, String p256dh, String auth) {
        // Avoid duplicates
        if (subscriptionRepository.findByUserIdAndEndpoint(userId, endpoint).isPresent()) {
            return;
        }
        var sub = new PushSubscription();
        sub.setUserId(userId);
        sub.setEndpoint(endpoint);
        sub.setP256dhKey(p256dh);
        sub.setAuthKey(auth);
        subscriptionRepository.save(sub);
    }

    @Transactional
    public void unsubscribe(UUID userId, String endpoint) {
        subscriptionRepository.deleteByUserIdAndEndpoint(userId, endpoint);
    }

    public void sendPushToUser(UUID userId, String title, String body, String url) {
        List<PushSubscription> subs = subscriptionRepository.findByUserId(userId);
        if (subs.isEmpty()) return;

        String payload = buildPayload(title, body, url);

        for (PushSubscription sub : subs) {
            try {
                var notification = new Notification(
                        sub.getEndpoint(),
                        sub.getP256dhKey(),
                        sub.getAuthKey(),
                        payload
                );
                pushService.send(notification);
            } catch (Exception e) {
                log.warn("Failed to send push to endpoint {}: {}", sub.getEndpoint(), e.getMessage());
                // If endpoint is gone (410 Gone), remove it
                if (e.getMessage() != null && e.getMessage().contains("410")) {
                    try {
                        subscriptionRepository.delete(sub);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private String buildPayload(String title, String body, String url) {
        return "{\"title\":" + jsonEscape(title) +
                ",\"body\":" + jsonEscape(body) +
                (url != null ? ",\"url\":" + jsonEscape(url) : "") +
                "}";
    }

    private String jsonEscape(String value) {
        if (value == null) return "null";
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }
}
