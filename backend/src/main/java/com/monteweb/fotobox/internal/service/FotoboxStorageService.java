package com.monteweb.fotobox.internal.service;

import io.minio.GetObjectArgs;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.MinioClient;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "monteweb.modules", name = "fotobox.enabled", havingValue = "true")
public class FotoboxStorageService {

    private static final Logger log = LoggerFactory.getLogger(FotoboxStorageService.class);
    private static final int THUMBNAIL_SIZE = 400;
    private static final float THUMBNAIL_QUALITY = 0.8f;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final MinioClient minioClient;
    private final String bucket;

    public FotoboxStorageService(MinioClient minioClient,
                                 @Value("${monteweb.storage.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    /**
     * Validates the content type of the uploaded file by checking magic bytes.
     */
    public String validateAndDetectContentType(MultipartFile file) {
        // Detect actual content type from magic bytes, ignoring declared Content-Type
        try {
            byte[] header = new byte[12];
            try (InputStream is = file.getInputStream()) {
                int read = is.read(header);
                if (read < 4) {
                    throw new IllegalArgumentException("File too small to be a valid image");
                }
            }

            String detectedType;
            if (isJpeg(header)) {
                detectedType = "image/jpeg";
            } else if (isPng(header)) {
                detectedType = "image/png";
            } else if (isGif(header)) {
                detectedType = "image/gif";
            } else if (isWebP(header)) {
                detectedType = "image/webp";
            } else {
                throw new IllegalArgumentException("File content does not match a valid image format. Allowed: JPEG, PNG, WebP, GIF");
            }
            return detectedType;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not validate file content: " + e.getMessage());
        }
    }

    /**
     * Uploads the original image to MinIO.
     * Returns the storage path (object key).
     */
    public String uploadOriginal(UUID roomId, UUID threadId, UUID imageId, String extension,
                                 MultipartFile file, String contentType) {
        String objectKey = buildOriginalPath(roomId, threadId, imageId, extension);
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(contentType)
                    .build());
            log.debug("Uploaded fotobox image to {}/{}", bucket, objectKey);
            return objectKey;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload image to storage: " + e.getMessage(), e);
        }
    }

    /**
     * Generates and uploads a thumbnail.
     * Returns the thumbnail storage path.
     */
    public String uploadThumbnail(UUID roomId, UUID threadId, UUID imageId, String extension,
                                  MultipartFile file) {
        String thumbKey = buildThumbnailPath(roomId, threadId, imageId, extension);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Thumbnails.of(file.getInputStream())
                    .size(THUMBNAIL_SIZE, THUMBNAIL_SIZE)
                    .outputFormat("jpeg")
                    .outputQuality(THUMBNAIL_QUALITY)
                    .toOutputStream(baos);

            byte[] thumbBytes = baos.toByteArray();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(thumbKey)
                    .stream(new ByteArrayInputStream(thumbBytes), thumbBytes.length, -1)
                    .contentType("image/jpeg")
                    .build());
            log.debug("Uploaded fotobox thumbnail to {}/{}", bucket, thumbKey);
            return thumbKey;
        } catch (Exception e) {
            log.warn("Failed to generate thumbnail for image {}: {}", imageId, e.getMessage());
            return null;
        }
    }

    /**
     * Returns an InputStream for downloading from MinIO.
     */
    public InputStream download(String storagePath) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(storagePath)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file from storage: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a file from MinIO storage.
     */
    public void delete(String storagePath) {
        if (storagePath == null) return;
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(storagePath)
                    .build());
            log.debug("Deleted fotobox file {}/{}", bucket, storagePath);
        } catch (Exception e) {
            log.warn("Failed to delete file from storage {}: {}", storagePath, e.getMessage());
        }
    }

    public static String extensionFromContentType(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> "bin";
        };
    }

    private String buildOriginalPath(UUID roomId, UUID threadId, UUID imageId, String extension) {
        return "rooms/" + roomId + "/fotobox/" + threadId + "/" + imageId + "." + extension;
    }

    private String buildThumbnailPath(UUID roomId, UUID threadId, UUID imageId, String extension) {
        return "rooms/" + roomId + "/fotobox/" + threadId + "/thumbs/" + imageId + ".jpg";
    }

    private boolean isJpeg(byte[] header) {
        return header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF;
    }

    private boolean isPng(byte[] header) {
        return header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47;
    }

    private boolean isGif(byte[] header) {
        return header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46;
    }

    private boolean isWebP(byte[] header) {
        return header.length >= 12
                && header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50;
    }
}
