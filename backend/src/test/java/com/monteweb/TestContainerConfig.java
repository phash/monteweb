package com.monteweb;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Testcontainers configuration for integration tests.
 * Provides PostgreSQL and Redis containers when running locally.
 * In CI, containers are provided as service containers and this config is
 * disabled via -Dtestcontainers.enabled=false.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainerConfig {

    @Bean
    @ServiceConnection
    @SuppressWarnings("resource")
    @ConditionalOnProperty(name = "testcontainers.enabled", havingValue = "true", matchIfMissing = true)
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("monteweb_test")
                .withUsername("test")
                .withPassword("test");
    }

    @Bean
    @ServiceConnection(name = "redis")
    @SuppressWarnings("resource")
    @ConditionalOnProperty(name = "testcontainers.enabled", havingValue = "true", matchIfMissing = true)
    public GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
    }
}
