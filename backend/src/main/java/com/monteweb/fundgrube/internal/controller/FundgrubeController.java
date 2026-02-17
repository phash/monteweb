package com.monteweb.fundgrube.internal.controller;

import com.monteweb.fundgrube.FundgrubeImageInfo;
import com.monteweb.fundgrube.FundgrubeItemInfo;
import com.monteweb.fundgrube.internal.dto.ClaimItemRequest;
import com.monteweb.fundgrube.internal.dto.CreateItemRequest;
import com.monteweb.fundgrube.internal.dto.UpdateItemRequest;
import com.monteweb.fundgrube.internal.service.FundgrubeService;
import com.monteweb.fundgrube.internal.service.FundgrubeStorageService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.util.SecurityUtils;
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
@RequestMapping("/api/v1/fundgrube")
@ConditionalOnProperty(prefix = "monteweb.modules", name = "fundgrube.enabled", havingValue = "true")
@RequiredArgsConstructor
public class FundgrubeController {

    private final FundgrubeService fundgrubeService;
    private final FundgrubeStorageService storageService;

    // ---- Items ----

    @GetMapping("/items")
    public ApiResponse<List<FundgrubeItemInfo>> listItems(
            @RequestParam(required = false) UUID sectionId) {
        SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(fundgrubeService.listItems(sectionId));
    }

    @GetMapping("/items/{itemId}")
    public ApiResponse<FundgrubeItemInfo> getItem(@PathVariable UUID itemId) {
        SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(fundgrubeService.getItem(itemId));
    }

    @PostMapping("/items")
    public ApiResponse<FundgrubeItemInfo> createItem(@Valid @RequestBody CreateItemRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(fundgrubeService.createItem(userId, request));
    }

    @PutMapping("/items/{itemId}")
    public ApiResponse<FundgrubeItemInfo> updateItem(
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateItemRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(fundgrubeService.updateItem(userId, itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<Void> deleteItem(@PathVariable UUID itemId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        fundgrubeService.deleteItem(userId, itemId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/items/{itemId}/claim")
    public ApiResponse<FundgrubeItemInfo> claimItem(
            @PathVariable UUID itemId,
            @Valid @RequestBody ClaimItemRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(fundgrubeService.claimItem(userId, itemId, request));
    }

    // ---- Images ----

    @PostMapping("/items/{itemId}/images")
    public ApiResponse<List<FundgrubeImageInfo>> uploadImages(
            @PathVariable UUID itemId,
            @RequestParam("files") List<MultipartFile> files) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(fundgrubeService.uploadImages(userId, itemId, files));
    }

    @DeleteMapping("/images/{imageId}")
    public ApiResponse<Void> deleteImage(@PathVariable UUID imageId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        fundgrubeService.deleteImage(userId, imageId);
        return ApiResponse.ok(null);
    }

    // ---- Image delivery (JWT via ?token= for <img> tags) ----

    @GetMapping("/images/{imageId}")
    public ResponseEntity<InputStreamResource> getImage(@PathVariable UUID imageId) {
        SecurityUtils.requireCurrentUserId();
        var image = fundgrubeService.requireImageForDownload(imageId);
        var stream = storageService.download(image.getStoragePath());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=86400")
                .body(new InputStreamResource(stream));
    }

    @GetMapping("/images/{imageId}/thumbnail")
    public ResponseEntity<InputStreamResource> getThumbnail(@PathVariable UUID imageId) {
        SecurityUtils.requireCurrentUserId();
        var image = fundgrubeService.requireImageForDownload(imageId);
        String path = image.getThumbnailPath() != null ? image.getThumbnailPath() : image.getStoragePath();
        var stream = storageService.download(path);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=86400")
                .body(new InputStreamResource(stream));
    }
}
