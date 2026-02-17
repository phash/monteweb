package com.monteweb.fundgrube.internal.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
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
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "monteweb.modules", name = "fundgrube.enabled", havingValue = "true")
public class FundgrubeStorageService {

    private static final Logger log = LoggerFactory.getLogger(FundgrubeStorageService.class);
    private static final int THUMBNAIL_SIZE = 400;
    private static final float THUMBNAIL_QUALITY = 0.8f;

    private final MinioClient minioClient;
    private final String bucket;

    public FundgrubeStorageService(MinioClient minioClient,
                                   @Value("${monteweb.storage.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    public String validateAndDetectContentType(MultipartFile file) {
        try {
            byte[] header = new byte[12];
            try (InputStream is = file.getInputStream()) {
                int read = is.read(header);
                if (read < 4) throw new IllegalArgumentException("File too small to be a valid image");
            }
            if (isJpeg(header)) return "image/jpeg";
            if (isPng(header)) return "image/png";
            if (isGif(header)) return "image/gif";
            if (isWebP(header)) return "image/webp";
            throw new IllegalArgumentException("Not a valid image format. Allowed: JPEG, PNG, WebP, GIF");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not validate file: " + e.getMessage());
        }
    }

    public String uploadOriginal(UUID itemId, UUID imageId, String extension,
                                 MultipartFile file, String contentType) {
        String key = buildOriginalPath(itemId, imageId, extension);
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket).object(key)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(contentType).build());
            return key;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload image: " + e.getMessage(), e);
        }
    }

    public String uploadThumbnail(UUID itemId, UUID imageId, MultipartFile file) {
        String key = buildThumbnailPath(itemId, imageId);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Thumbnails.of(file.getInputStream())
                    .size(THUMBNAIL_SIZE, THUMBNAIL_SIZE)
                    .outputFormat("jpeg")
                    .outputQuality(THUMBNAIL_QUALITY)
                    .toOutputStream(baos);
            byte[] bytes = baos.toByteArray();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket).object(key)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType("image/jpeg").build());
            return key;
        } catch (Exception e) {
            log.warn("Failed to generate thumbnail for fundgrube image {}: {}", imageId, e.getMessage());
            return null;
        }
    }

    public InputStream download(String storagePath) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket).object(storagePath).build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file: " + e.getMessage(), e);
        }
    }

    public void delete(String storagePath) {
        if (storagePath == null) return;
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket).object(storagePath).build());
        } catch (Exception e) {
            log.warn("Failed to delete fundgrube file {}: {}", storagePath, e.getMessage());
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

    private String buildOriginalPath(UUID itemId, UUID imageId, String extension) {
        return "fundgrube/" + itemId + "/" + imageId + "." + extension;
    }

    private String buildThumbnailPath(UUID itemId, UUID imageId) {
        return "fundgrube/" + itemId + "/thumbs/" + imageId + ".jpg";
    }

    private boolean isJpeg(byte[] h) { return h[0] == (byte)0xFF && h[1] == (byte)0xD8 && h[2] == (byte)0xFF; }
    private boolean isPng(byte[] h) { return h[0] == (byte)0x89 && h[1] == 0x50 && h[2] == 0x4E && h[3] == 0x47; }
    private boolean isGif(byte[] h) { return h[0] == 0x47 && h[1] == 0x49 && h[2] == 0x46; }
    private boolean isWebP(byte[] h) {
        return h.length >= 12
                && h[0] == 0x52 && h[1] == 0x49 && h[2] == 0x46 && h[3] == 0x46
                && h[8] == 0x57 && h[9] == 0x45 && h[10] == 0x42 && h[11] == 0x50;
    }
}
