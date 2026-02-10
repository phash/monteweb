package com.monteweb.files.internal.controller;

import com.monteweb.files.FileInfo;
import com.monteweb.files.FolderInfo;
import com.monteweb.files.internal.service.FileService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.util.SecurityUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms/{roomId}/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    // ---- Files ----

    @GetMapping
    public ResponseEntity<ApiResponse<List<FileInfo>>> listFiles(
            @PathVariable UUID roomId,
            @RequestParam(required = false) UUID folderId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var files = fileService.listFiles(roomId, folderId, userId);
        return ResponseEntity.ok(ApiResponse.ok(files));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileInfo>> uploadFile(
            @PathVariable UUID roomId,
            @RequestParam(required = false) UUID folderId,
            @RequestParam("file") MultipartFile file) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var fileInfo = fileService.uploadFile(roomId, folderId, userId, file);
        return ResponseEntity.ok(ApiResponse.ok(fileInfo));
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<InputStreamResource> downloadFile(
            @PathVariable UUID roomId,
            @PathVariable UUID fileId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var metadata = fileService.getFileMetadata(roomId, fileId, userId);
        var stream = fileService.downloadFile(roomId, fileId, userId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + metadata.getOriginalName() + "\"")
                .contentType(MediaType.parseMediaType(
                        metadata.getContentType() != null ? metadata.getContentType() : "application/octet-stream"))
                .contentLength(metadata.getFileSize())
                .body(new InputStreamResource(stream));
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @PathVariable UUID roomId,
            @PathVariable UUID fileId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        fileService.deleteFile(roomId, fileId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ---- Folders ----

    @GetMapping("/folders")
    public ResponseEntity<ApiResponse<List<FolderInfo>>> listFolders(
            @PathVariable UUID roomId,
            @RequestParam(required = false) UUID parentId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var folders = fileService.listFolders(roomId, parentId, userId);
        return ResponseEntity.ok(ApiResponse.ok(folders));
    }

    @PostMapping("/folders")
    public ResponseEntity<ApiResponse<FolderInfo>> createFolder(
            @PathVariable UUID roomId,
            @RequestBody CreateFolderRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var folder = fileService.createFolder(roomId, request.parentId(), request.name(), userId);
        return ResponseEntity.ok(ApiResponse.ok(folder));
    }

    @DeleteMapping("/folders/{folderId}")
    public ResponseEntity<ApiResponse<Void>> deleteFolder(
            @PathVariable UUID roomId,
            @PathVariable UUID folderId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        fileService.deleteFolder(roomId, folderId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    public record CreateFolderRequest(UUID parentId, String name) {
    }
}
