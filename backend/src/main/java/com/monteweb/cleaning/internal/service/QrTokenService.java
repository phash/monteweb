package com.monteweb.cleaning.internal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

/**
 * Generates and validates HMAC-signed QR tokens for cleaning slot check-ins.
 * Token format: {slotId}:{timestamp}:{signature}
 * Tokens are valid for the duration of the cleaning slot.
 */
@Service
@ConditionalOnProperty(prefix = "monteweb.modules.cleaning", name = "enabled", havingValue = "true")
public class QrTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${monteweb.cleaning.qr-secret:monteweb-cleaning-qr-default-secret}")
    private String secret;

    /**
     * Generates a QR token for a cleaning slot.
     */
    public String generateToken(UUID slotId) {
        String payload = slotId.toString() + ":" + System.currentTimeMillis();
        String signature = sign(payload);
        return payload + ":" + signature;
    }

    /**
     * Validates a QR token and extracts the slot ID.
     * Returns the slot ID if valid, null otherwise.
     */
    public UUID validateToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        String[] parts = token.split(":");
        // UUID has dashes, so slotId parts + timestamp + signature = more than 3 parts
        // Format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx:timestamp:signature
        if (parts.length < 3) {
            return null;
        }

        // Reconstruct: everything before the last two colon-separated parts is not the format we use
        // Actually our format is: {uuid}:{timestamp}:{base64signature}
        // UUID contains dashes, not colons, so split by ":" gives us:
        // [uuid, timestamp, signature]
        int lastColon = token.lastIndexOf(':');
        int secondLastColon = token.lastIndexOf(':', lastColon - 1);

        if (lastColon < 0 || secondLastColon < 0) {
            return null;
        }

        String payload = token.substring(0, lastColon);
        String providedSignature = token.substring(lastColon + 1);

        String expectedSignature = sign(payload);
        if (!expectedSignature.equals(providedSignature)) {
            return null;
        }

        String slotIdStr = token.substring(0, secondLastColon);
        try {
            return UUID.fromString(slotIdStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate HMAC signature", e);
        }
    }
}
