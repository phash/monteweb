package com.monteweb.files.internal.controller;

import com.monteweb.files.internal.model.RoomFile;
import com.monteweb.files.internal.model.WopiToken;
import com.monteweb.files.internal.repository.RoomFileRepository;
import com.monteweb.files.internal.service.FileStorageService;
import com.monteweb.files.internal.service.WopiTokenService;
import com.monteweb.shared.util.FileValidationUtils;
import com.monteweb.user.UserModuleApi;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * WOPI protocol endpoints for ONLYOFFICE integration.
 * These endpoints use token-based auth (NOT JWT) as they are called by the ONLYOFFICE server.
 */
@RestController
@RequestMapping("/wopi/files")
@ConditionalOnProperty(prefix = "monteweb.modules.files", name = "enabled", havingValue = "true")
public class WopiController {

    private static final Logger log = LoggerFactory.getLogger(WopiController.class);

    private final WopiTokenService wopiTokenService;
    private final RoomFileRepository fileRepository;
    private final FileStorageService storageService;
    private final UserModuleApi userModuleApi;
    private final MinioClient minioClient;
    private final String bucket;

    public WopiController(WopiTokenService wopiTokenService,
                          RoomFileRepository fileRepository,
                          FileStorageService storageService,
                          UserModuleApi userModuleApi,
                          MinioClient minioClient,
                          @Value("${monteweb.storage.bucket}") String bucket) {
        this.wopiTokenService = wopiTokenService;
        this.fileRepository = fileRepository;
        this.storageService = storageService;
        this.userModuleApi = userModuleApi;
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    /**
     * WOPI CheckFileInfo — returns metadata about the file.
     */
    @GetMapping("/{token}")
    public ResponseEntity<Map<String, Object>> checkFileInfo(@PathVariable String token) {
        WopiToken wopiToken = wopiTokenService.validateToken(token);
        if (wopiToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        RoomFile file = fileRepository.findById(wopiToken.getFileId()).orElse(null);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        String userName = userModuleApi.findById(wopiToken.getUserId())
                .map(u -> u.firstName() + " " + u.lastName())
                .orElse("Unknown");

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("BaseFileName", file.getOriginalName());
        info.put("Size", file.getFileSize());
        info.put("OwnerId", file.getUploadedBy().toString());
        info.put("UserId", wopiToken.getUserId().toString());
        info.put("UserFriendlyName", userName);
        info.put("UserCanWrite", "EDIT".equals(wopiToken.getPermissions()));
        info.put("UserCanNotWriteRelative", true);
        info.put("SupportsUpdate", "EDIT".equals(wopiToken.getPermissions()));
        info.put("SupportsLocks", false);
        info.put("Version", String.valueOf(file.getCreatedAt().toEpochMilli()));

        return ResponseEntity.ok(info);
    }

    /**
     * WOPI GetFile — streams the file content from MinIO.
     */
    @GetMapping("/{token}/contents")
    public ResponseEntity<InputStreamResource> getFile(@PathVariable String token) {
        WopiToken wopiToken = wopiTokenService.validateToken(token);
        if (wopiToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        RoomFile file = fileRepository.findById(wopiToken.getFileId()).orElse(null);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        InputStream stream = storageService.download(file.getStoragePath());
        String safeFilename = FileValidationUtils.sanitizeContentDispositionFilename(file.getOriginalName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + safeFilename + "\"")
                .contentType(MediaType.parseMediaType(
                        file.getContentType() != null ? file.getContentType() : "application/octet-stream"))
                .contentLength(file.getFileSize())
                .body(new InputStreamResource(stream));
    }

    /**
     * WOPI PutFile — saves the uploaded content back to MinIO.
     */
    @PostMapping("/{token}/contents")
    public ResponseEntity<Void> putFile(@PathVariable String token, InputStream body) {
        WopiToken wopiToken = wopiTokenService.validateToken(token);
        if (wopiToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!"EDIT".equals(wopiToken.getPermissions())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        RoomFile file = fileRepository.findById(wopiToken.getFileId()).orElse(null);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            // Overwrite the file in MinIO
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(file.getStoragePath())
                    .stream(body, -1, 10485760) // unknown size, 10MB part size
                    .contentType(file.getContentType())
                    .build());

            log.debug("WOPI PutFile: updated {} in MinIO", file.getStoragePath());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("WOPI PutFile failed for {}: {}", file.getStoragePath(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
