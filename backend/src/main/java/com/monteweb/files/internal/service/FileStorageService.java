package com.monteweb.files.internal.service;

import io.minio.*;
import io.minio.errors.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Low-level storage operations against MinIO (S3-compatible).
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final MinioClient minioClient;
    private final String bucket;

    public FileStorageService(MinioClient minioClient,
                              @Value("${monteweb.storage.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    /**
     * Uploads a file and returns the storage path (object key).
     */
    public String upload(UUID roomId, UUID folderId, String storedName, MultipartFile file) {
        String folderPart = folderId != null ? folderId.toString() + "/" : "";
        String objectKey = "rooms/" + roomId + "/files/" + folderPart + storedName;

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            log.debug("Uploaded file to {}/{}", bucket, objectKey);
            return objectKey;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to storage: " + e.getMessage(), e);
        }
    }

    /**
     * Returns an InputStream for downloading the file.
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
     * Deletes a file from storage.
     */
    public void delete(String storagePath) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(storagePath)
                    .build());
            log.debug("Deleted file {}/{}", bucket, storagePath);
        } catch (Exception e) {
            log.warn("Failed to delete file from storage {}: {}", storagePath, e.getMessage());
        }
    }
}
