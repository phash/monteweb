package com.monteweb.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

/**
 * Validates that critical secrets are properly configured in non-dev/test environments.
 * Logs errors for every weak/default secret found, counts problems, and in production
 * (profile "prod" or "production") throws an {@link IllegalStateException} to abort
 * startup so the application never runs with insecure configuration.
 */
@Configuration
@Profile("!dev & !test")
public class SecretValidationConfig {

    private static final Logger log = LoggerFactory.getLogger(SecretValidationConfig.class);

    @Autowired
    private Environment environment;

    @Value("${monteweb.jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${monteweb.storage.access-key:}")
    private String minioAccessKey;

    @Value("${monteweb.storage.secret-key:}")
    private String minioSecretKey;

    @Value("${monteweb.cleaning.qr-secret:monteweb-cleaning-qr-default-secret}")
    private String qrSecret;

    @Value("${monteweb.encryption.secret:}")
    private String encryptionSecret;

    @EventListener(ApplicationReadyEvent.class)
    public void validateSecrets() {
        int problems = 0;

        if (jwtSecret.isBlank() || jwtSecret.contains("dev-only") || jwtSecret.startsWith("change-this-to-a-secure")) {
            log.error("SECURITY: JWT_SECRET is not configured or uses the insecure example placeholder. Set JWT_SECRET environment variable.");
            problems++;
        } else if (jwtSecret.length() < 64) {
            log.error("SECURITY: JWT_SECRET is too short ({}). Must be at least 64 characters.", jwtSecret.length());
            problems++;
        }

        if ("changeme".equals(dbPassword) || dbPassword.isBlank()) {
            log.warn("SECURITY: Database password is not configured or uses the insecure default 'changeme'. Set DB_PASSWORD environment variable.");
            problems++;
        }

        if ("changeme".equals(redisPassword) || redisPassword.isBlank()) {
            log.warn("SECURITY: Redis password is not configured or uses the insecure default 'changeme'. Set REDIS_PASSWORD environment variable.");
            problems++;
        }

        if ("minioadmin".equals(minioAccessKey) || "minioadmin".equals(minioSecretKey)) {
            log.warn("SECURITY: MinIO credentials use the insecure defaults. Set MINIO_ACCESS_KEY and MINIO_SECRET_KEY environment variables.");
            problems++;
        }

        if (qrSecret.isBlank() || "monteweb-cleaning-qr-default-secret".equals(qrSecret)) {
            log.error("SECURITY: monteweb.cleaning.qr-secret is not set or uses the insecure default value!");
            problems++;
        }

        if (encryptionSecret.isBlank()) {
            // Falls back to JWT_SECRET (documented behaviour) — warn but do not abort startup.
            log.warn("SECURITY: ENCRYPTION_SECRET is not set. AES key falls back to JWT_SECRET — set a separate ENCRYPTION_SECRET (64+ chars).");
        } else if (encryptionSecret.length() < 64) {
            log.warn("SECURITY: ENCRYPTION_SECRET is too short ({} chars). Use at least 64 characters.", encryptionSecret.length());
            problems++;
        } else if (encryptionSecret.equals(jwtSecret)) {
            log.error("SECURITY: ENCRYPTION_SECRET must differ from JWT_SECRET.");
            problems++;
        }

        if (problems > 0) {
            // This bean is @Profile("!dev & !test"), so it only runs in real
            // (non-dev/non-test) environments. Fail closed: never boot a real
            // deployment with weak/default secrets. For local development with
            // relaxed validation, activate the 'dev' profile (SPRING_PROFILES_ACTIVE=dev).
            throw new IllegalStateException(
                    "SECURITY: " + problems + " secret validation failure(s) detected. "
                            + "Fix the configuration before starting, or set SPRING_PROFILES_ACTIVE=dev for local development.");
        } else {
            log.info("Secret validation passed: all critical secrets are configured.");
        }
    }
}
