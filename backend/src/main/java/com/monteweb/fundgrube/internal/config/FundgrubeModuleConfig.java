package com.monteweb.fundgrube.internal.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "monteweb.modules", name = "fundgrube.enabled", havingValue = "true")
@ComponentScan(basePackages = "com.monteweb.fundgrube.internal")
public class FundgrubeModuleConfig {

    private static final Logger log = LoggerFactory.getLogger(FundgrubeModuleConfig.class);

    @Bean
    @ConditionalOnMissingBean(MinioClient.class)
    public MinioClient fundgrubeMinioClient(
            @Value("${monteweb.storage.endpoint}") String endpoint,
            @Value("${monteweb.storage.access-key}") String accessKey,
            @Value("${monteweb.storage.secret-key}") String secretKey,
            @Value("${monteweb.storage.bucket}") String bucket) {

        var client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("Could not verify/create MinIO bucket '{}': {}", bucket, e.getMessage());
        }
        return client;
    }
}
