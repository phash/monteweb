package com.monteweb.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;

/**
 * Validates that critical secrets are properly configured in non-dev/test environments.
 * Fails fast at startup if insecure defaults are detected.
 */
@Configuration
@Profile("!dev & !test")
public class SecretValidationConfig {

    private static final Logger log = LoggerFactory.getLogger(SecretValidationConfig.class);

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

    @EventListener(ApplicationReadyEvent.class)
    public void validateSecrets() {
        boolean hasWarnings = false;

        if (jwtSecret.isBlank() || jwtSecret.contains("dev-only")) {
            log.error("SECURITY: JWT_SECRET is not configured or uses the insecure default. Set JWT_SECRET environment variable.");
            hasWarnings = true;
        } else if (jwtSecret.length() < 64) {
            log.error("SECURITY: JWT_SECRET is too short ({}). Must be at least 64 characters.", jwtSecret.length());
            hasWarnings = true;
        }

        if ("changeme".equals(dbPassword) || dbPassword.isBlank()) {
            log.warn("SECURITY: Database password is not configured or uses the insecure default 'changeme'. Set DB_PASSWORD environment variable.");
            hasWarnings = true;
        }

        if ("changeme".equals(redisPassword) || redisPassword.isBlank()) {
            log.warn("SECURITY: Redis password is not configured or uses the insecure default 'changeme'. Set REDIS_PASSWORD environment variable.");
            hasWarnings = true;
        }

        if ("minioadmin".equals(minioAccessKey) || "minioadmin".equals(minioSecretKey)) {
            log.warn("SECURITY: MinIO credentials use the insecure defaults. Set MINIO_ACCESS_KEY and MINIO_SECRET_KEY environment variables.");
            hasWarnings = true;
        }

        if (hasWarnings) {
            log.error("SECURITY: One or more secrets are not properly configured. Fix before deploying to production.");
        } else {
            log.info("Secret validation passed: all critical secrets are configured.");
        }
    }
}
