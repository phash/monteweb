package com.monteweb.fotobox.internal.controller;

import com.monteweb.fotobox.FotoboxImageInfo;
import com.monteweb.fotobox.FotoboxThreadInfo;
import com.monteweb.fotobox.internal.dto.*;
import com.monteweb.fotobox.internal.service.FotoboxService;
import com.monteweb.fotobox.internal.service.FotoboxStorageService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.exception.ForbiddenException;
import com.monteweb.shared.util.SecurityUtils;
import com.monteweb.user.UserModuleApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@ConditionalOnProperty(prefix = "monteweb.modules", name = "fotobox.enabled", havingValue = "true")
@RequiredArgsConstructor
public class FotoboxController {

    private final FotoboxService fotoboxService;
    private final FotoboxStorageService storageService;
    private final UserModuleApi userModuleApi;

    // --- Settings ---

    @GetMapping("/rooms/{roomId}/fotobox/settings")
    public ApiResponse<FotoboxRoomSettingsResponse> getSettings(@PathVariable UUID roomId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(fotoboxService.getSettings(userId, roomId));
    }

    @PutMapping("/rooms/{roomId}/fotobox/settings")
    public ApiResponse<FotoboxRoomSettingsResponse> updateSettings(
            @PathVariable UUID roomId,
            @Valid @RequestBody UpdateSettingsRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(fotoboxService.updateSettings(userId, roomId, request));
    }

    // --- Threads ---

    @GetMapping("/rooms/{roomId}/fotobox/threads")
    public ApiResponse<List<FotoboxThreadInfo>> getThreads(@PathVariable UUID roomId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(fotoboxService.getThreads(userId, roomId));
    }

    @GetMapping("/rooms/{roomId}/fotobox/threads/{threadId}")
    public ApiResponse<FotoboxThreadInfo> getThread(
            @PathVariable UUID roomId,
            @PathVariable UUID threadId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(fotoboxService.getThread(userId, roomId, threadId));
    }

    @GetMapping("/rooms/{roomId}/fotobox/threads/{threadId}/images")
    public ApiResponse<List<FotoboxImageInfo>> getThreadImages(
            @PathVariable UUID roomId,
            @PathVariable UUID threadId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(fotoboxService.getThreadImages(userId, roomId, threadId));
    }

    @PostMapping("/rooms/{roomId}/fotobox/threads")
    public ApiResponse<FotoboxThreadInfo> createThread(
            @PathVariable UUID roomId,
            @Valid @RequestBody CreateThreadRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(fotoboxService.createThread(userId, roomId, request));
    }

    @PutMapping("/rooms/{roomId}/fotobox/threads/{threadId}")
    public ApiResponse<FotoboxThreadInfo> updateThread(
            @PathVariable UUID roomId,
            @PathVariable UUID threadId,
            @Valid @RequestBody UpdateThreadRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(fotoboxService.updateThread(userId, roomId, threadId, request));
    }

    @DeleteMapping("/rooms/{roomId}/fotobox/threads/{threadId}")
    public ApiResponse<Void> deleteThread(
            @PathVariable UUID roomId,
            @PathVariable UUID threadId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        fotoboxService.deleteThread(userId, roomId, threadId);
        return ApiResponse.ok(null);
    }

    // --- Images ---

    @PostMapping("/rooms/{roomId}/fotobox/threads/{threadId}/images")
    public ApiResponse<List<FotoboxImageInfo>> uploadImages(
            @PathVariable UUID roomId,
            @PathVariable UUID threadId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "caption", required = false) String caption) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        if (!userModuleApi.hasActiveConsent(userId, "PHOTO_CONSENT")) {
            throw new ForbiddenException("PHOTO_CONSENT required to upload images to Fotobox");
        }
        return ApiResponse.ok(fotoboxService.uploadImages(userId, roomId, threadId, files, caption));
    }

    @PutMapping("/fotobox/images/{imageId}")
    public ApiResponse<FotoboxImageInfo> updateImage(
            @PathVariable UUID imageId,
            @Valid @RequestBody UpdateImageRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(fotoboxService.updateImage(userId, imageId, request));
    }

    @DeleteMapping("/fotobox/images/{imageId}")
    public ApiResponse<Void> deleteImage(@PathVariable UUID imageId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        fotoboxService.deleteImage(userId, imageId);
        return ApiResponse.ok(null);
    }

    // --- Image delivery ---

    @GetMapping("/fotobox/images/{imageId}")
    public ResponseEntity<InputStreamResource> getImage(@PathVariable UUID imageId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var image = fotoboxService.getImageForDownload(userId, imageId);
        var stream = storageService.download(image.getStoragePath());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=86400")
                .body(new InputStreamResource(stream));
    }

    @GetMapping("/fotobox/images/{imageId}/thumbnail")
    public ResponseEntity<InputStreamResource> getThumbnail(@PathVariable UUID imageId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var image = fotoboxService.getImageForDownload(userId, imageId);
        String thumbPath = image.getThumbnailPath();
        if (thumbPath == null) {
            // Fall back to original
            thumbPath = image.getStoragePath();
        }
        var stream = storageService.download(thumbPath);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=86400")
                .body(new InputStreamResource(stream));
    }
}
