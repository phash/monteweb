package com.monteweb.search.internal.controller;

import com.monteweb.search.SearchResult;
import com.monteweb.search.internal.service.SearchService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.util.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SearchResult>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "20") int limit) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        int effectiveLimit = Math.min(Math.max(limit, 1), 50);
        var results = searchService.search(q, type, effectiveLimit, userId);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }
}
