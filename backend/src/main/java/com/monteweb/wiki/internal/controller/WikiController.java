package com.monteweb.wiki.internal.controller;

import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.util.SecurityUtils;
import com.monteweb.wiki.internal.dto.*;
import com.monteweb.wiki.internal.service.WikiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms/{roomId}/wiki")
@ConditionalOnProperty(prefix = "monteweb.modules", name = "wiki.enabled", havingValue = "true")
@RequiredArgsConstructor
public class WikiController {

    private final WikiService wikiService;

    @GetMapping
    public ApiResponse<List<WikiPageSummary>> getPageTree(@PathVariable UUID roomId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(wikiService.getPageTree(userId, roomId));
    }

    @GetMapping("/pages/{slug}")
    public ApiResponse<WikiPageResponse> getPage(
            @PathVariable UUID roomId,
            @PathVariable String slug) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(wikiService.getPage(userId, roomId, slug));
    }

    @PostMapping("/pages")
    public ApiResponse<WikiPageResponse> createPage(
            @PathVariable UUID roomId,
            @Valid @RequestBody CreatePageRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(wikiService.createPage(roomId, userId, request));
    }

    @PutMapping("/pages/{pageId}")
    public ApiResponse<WikiPageResponse> updatePage(
            @PathVariable UUID roomId,
            @PathVariable UUID pageId,
            @Valid @RequestBody UpdatePageRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(wikiService.updatePage(pageId, userId, request));
    }

    @DeleteMapping("/pages/{pageId}")
    public ApiResponse<Void> deletePage(
            @PathVariable UUID roomId,
            @PathVariable UUID pageId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        wikiService.deletePage(pageId, userId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/pages/{pageId}/versions")
    public ApiResponse<List<WikiPageVersionResponse>> getVersions(
            @PathVariable UUID roomId,
            @PathVariable UUID pageId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(wikiService.getVersions(userId, pageId));
    }

    @GetMapping("/versions/{versionId}")
    public ApiResponse<WikiPageVersionResponse> getVersion(
            @PathVariable UUID roomId,
            @PathVariable UUID versionId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(wikiService.getVersion(userId, roomId, versionId));
    }

    @GetMapping("/search")
    public ApiResponse<List<WikiPageSummary>> searchPages(
            @PathVariable UUID roomId,
            @RequestParam("q") String query) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(wikiService.searchPages(userId, roomId, query));
    }
}
