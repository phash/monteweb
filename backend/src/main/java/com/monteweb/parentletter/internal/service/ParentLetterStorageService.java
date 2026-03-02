package com.monteweb.parentletter.internal.service;

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

/**
 * Handles MinIO storage for parent letter letterhead images.
 * Storage path pattern: parentletter/config/{sectionId|global}/letterhead.{ext}
 */
@Service
@ConditionalOnProperty(prefix = "monteweb.modules", name = "parentletter.enabled", havingValue = "true")
public class ParentLetterStorageService {

    private static final Logger log = LoggerFactory.getLogger(ParentLetterStorageService.class);

    private static final long PART_SIZE = 5 * 1024 * 1024L; // 5 MB multipart threshold

    private final MinioClient minioClient;
    private final String bucket;

    public ParentLetterStorageService(MinioClient minioClient,
                                      @Value("${monteweb.storage.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    /**
     * Upload a letterhead image for a section (or the global config).
     *
     * @param sectionId null for the global letterhead
     * @param file      the uploaded image file
     * @return the MinIO storage path (to be persisted in the config)
     */
    public String uploadLetterhead(UUID sectionId, MultipartFile file) {
        String folder = sectionId != null ? sectionId.toString() : "global";
        String extension = resolveExtension(file);
        String storagePath = "parentletter/config/" + folder + "/letterhead." + extension;

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(storagePath)
                            .stream(inputStream, file.getSize(), PART_SIZE)
                            .contentType(resolveContentType(file))
                            .build()
            );
            log.info("Uploaded letterhead to MinIO: {}", storagePath);
            return storagePath;
        } catch (Exception e) {
            log.error("Failed to upload letterhead to MinIO: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload letterhead image: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a letterhead image from MinIO.
     *
     * @param storagePath the path returned by {@link #uploadLetterhead}
     */
    public void deleteLetterhead(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(storagePath)
                            .build()
            );
            log.info("Deleted letterhead from MinIO: {}", storagePath);
        } catch (Exception e) {
            log.warn("Failed to delete letterhead from MinIO ({}): {}", storagePath, e.getMessage());
        }
    }

    /**
     * Download a letterhead image as an InputStream.
     *
     * @param storagePath the path returned by {@link #uploadLetterhead}
     * @return InputStream of the image data
     */
    public InputStream getLetterhead(String storagePath) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(storagePath)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to download letterhead from MinIO ({}): {}", storagePath, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve letterhead image: " + e.getMessage(), e);
        }
    }

    // ---- Attachment storage ----

    public String uploadAttachment(UUID letterId, UUID attachmentId, MultipartFile file, String contentType) {
        String extension = resolveExtension(file);
        String storagePath = "parentletter/" + letterId + "/attachments/" + attachmentId + "." + extension;

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(storagePath)
                            .stream(inputStream, file.getSize(), PART_SIZE)
                            .contentType(contentType)
                            .build()
            );
            log.info("Uploaded attachment to MinIO: {}", storagePath);
            return storagePath;
        } catch (Exception e) {
            log.error("Failed to upload attachment to MinIO: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload attachment: " + e.getMessage(), e);
        }
    }

    public InputStream downloadAttachment(String storagePath) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(storagePath)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to download attachment from MinIO ({}): {}", storagePath, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve attachment: " + e.getMessage(), e);
        }
    }

    public void deleteAttachment(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) return;
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(storagePath)
                            .build()
            );
            log.info("Deleted attachment from MinIO: {}", storagePath);
        } catch (Exception e) {
            log.warn("Failed to delete attachment from MinIO ({}): {}", storagePath, e.getMessage());
        }
    }

    // ---- Helpers ----

    private String resolveExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
            return switch (ext) {
                case "jpg", "jpeg" -> "jpg";
                case "png" -> "png";
                case "svg" -> "svg";
                case "pdf" -> "pdf";
                default -> "bin";
            };
        }
        // Fallback: derive from content type
        String contentType = file.getContentType();
        if (contentType != null) {
            return switch (contentType) {
                case "image/jpeg" -> "jpg";
                case "image/png" -> "png";
                case "image/svg+xml" -> "svg";
                case "application/pdf" -> "pdf";
                default -> "bin";
            };
        }
        return "bin";
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null ? contentType : "application/octet-stream";
    }
}
