package com.monteweb.jobboard.internal.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "monteweb.modules.jobboard", name = "enabled", havingValue = "true")
public class JobStorageService {

    private static final Logger log = LoggerFactory.getLogger(JobStorageService.class);

    private final MinioClient minioClient;
    private final String bucket;

    public JobStorageService(MinioClient minioClient,
                             @Value("${monteweb.storage.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    public String upload(UUID jobId, UUID attachmentId, String extension,
                         MultipartFile file, String contentType) {
        String key = buildPath(jobId, attachmentId, extension);
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket).object(key)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(contentType).build());
            return key;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload job attachment: " + e.getMessage(), e);
        }
    }

    public InputStream download(String storagePath) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket).object(storagePath).build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to download job attachment: " + e.getMessage(), e);
        }
    }

    public void delete(String storagePath) {
        if (storagePath == null) return;
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket).object(storagePath).build());
        } catch (Exception e) {
            log.warn("Failed to delete job attachment {}: {}", storagePath, e.getMessage());
        }
    }

    public static String extensionFromContentType(String contentType) {
        if (contentType == null) return "bin";
        return switch (contentType) {
            case "application/pdf" -> "pdf";
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "application/msword" -> "doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx";
            case "application/vnd.ms-excel" -> "xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx";
            case "text/plain" -> "txt";
            default -> "bin";
        };
    }

    private String buildPath(UUID jobId, UUID attachmentId, String extension) {
        return "jobs/" + jobId + "/" + attachmentId + "." + extension;
    }
}
