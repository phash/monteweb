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
        // FATAL = cryptographic secrets whose compromise is catastrophic (token forgery,
        // decryptable 2FA secrets, forgeable QR check-ins). These abort startup.
        // WARN = internal-network infra credentials (DB/Redis/MinIO). These only warn, so
        //        rolling out this guard does not unexpectedly take down an existing
        //        deployment whose .env still carries the old defaults.
        int fatal = 0;
        int warnings = 0;

        if (jwtSecret.isBlank() || jwtSecret.contains("dev-only") || jwtSecret.startsWith("change-this-to-a-secure")) {
            log.error("SECURITY: JWT_SECRET is not configured or uses the insecure example placeholder. Set JWT_SECRET environment variable.");
            fatal++;
        } else if (jwtSecret.length() < 64) {
            log.error("SECURITY: JWT_SECRET is too short ({}). Must be at least 64 characters.", jwtSecret.length());
            fatal++;
        }

        if (qrSecret.isBlank() || "monteweb-cleaning-qr-default-secret".equals(qrSecret)) {
            log.error("SECURITY: monteweb.cleaning.qr-secret is not set or uses the insecure default value!");
            fatal++;
        }

        if (encryptionSecret.isBlank()) {
            // Falls back to JWT_SECRET (documented behaviour) — warn, do not abort.
            log.warn("SECURITY: ENCRYPTION_SECRET is not set. AES key falls back to JWT_SECRET — set a separate ENCRYPTION_SECRET (64+ chars).");
            warnings++;
        } else if (encryptionSecret.length() < 64) {
            log.error("SECURITY: ENCRYPTION_SECRET is set but too short ({} chars). Use at least 64 characters.", encryptionSecret.length());
            fatal++;
        } else if (encryptionSecret.equals(jwtSecret)) {
            log.error("SECURITY: ENCRYPTION_SECRET must differ from JWT_SECRET.");
            fatal++;
        }

        if ("changeme".equals(dbPassword) || dbPassword.isBlank()) {
            log.warn("SECURITY: Database password is not configured or uses the insecure default 'changeme'. Set DB_PASSWORD environment variable.");
            warnings++;
        }

        if ("changeme".equals(redisPassword) || redisPassword.isBlank()) {
            log.warn("SECURITY: Redis password is not configured or uses the insecure default 'changeme'. Set REDIS_PASSWORD environment variable.");
            warnings++;
        }

        if ("minioadmin".equals(minioAccessKey) || "minioadmin".equals(minioSecretKey)) {
            log.warn("SECURITY: MinIO credentials use the insecure defaults. Set MINIO_ACCESS_KEY and MINIO_SECRET_KEY environment variables.");
            warnings++;
        }

        if (fatal > 0) {
            // This bean is @Profile("!dev & !test"), so it only runs in real
            // (non-dev/non-test) environments. Fail closed on catastrophic secrets:
            // never boot a real deployment with a forgeable JWT/QR/encryption key.
            // For local development, activate the 'dev' profile (SPRING_PROFILES_ACTIVE=dev).
            throw new IllegalStateException(
                    "SECURITY: " + fatal + " critical secret validation failure(s) detected. "
                            + "Fix JWT_SECRET / ENCRYPTION_SECRET / cleaning QR secret before starting, "
                            + "or set SPRING_PROFILES_ACTIVE=dev for local development.");
        }
        if (warnings > 0) {
            log.warn("SECURITY WARNING: {} infrastructure credential(s) use weak/default values. "
                    + "These MUST be changed before production deployment.", warnings);
        } else {
            log.info("Secret validation passed: all critical secrets are configured.");
        }
    }
}
