package com.monteweb.feed.internal.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
public class FeedStorageService {

    private static final Logger log = LoggerFactory.getLogger(FeedStorageService.class);

    private final MinioClient minioClient;
    private final String bucket;

    public FeedStorageService(MinioClient minioClient,
                              @Value("${monteweb.storage.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    public String upload(UUID postId, UUID attachmentId, MultipartFile file) {
        String extension = getExtension(file.getOriginalFilename());
        String objectKey = "feed/" + postId + "/" + attachmentId + "." + extension;
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            log.debug("Uploaded feed attachment to {}/{}", bucket, objectKey);
            return objectKey;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload feed attachment: " + e.getMessage(), e);
        }
    }

    public InputStream download(String storagePath) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(storagePath)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to download feed attachment: " + e.getMessage(), e);
        }
    }

    public void delete(String storagePath) {
        if (storagePath == null) return;
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(storagePath)
                    .build());
            log.debug("Deleted feed attachment {}/{}", bucket, storagePath);
        } catch (Exception e) {
            log.warn("Failed to delete feed attachment {}: {}", storagePath, e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "bin";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "bin";
    }
}
