package com.monteweb.search.internal.controller;

import com.monteweb.search.internal.service.SolrIndexingService;
import com.monteweb.shared.dto.ApiResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/search")
@ConditionalOnProperty(prefix = "monteweb.modules.solr", name = "enabled", havingValue = "true")
public class SolrAdminController {

    private final SolrIndexingService indexingService;

    public SolrAdminController(SolrIndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @PostMapping("/reindex")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reindex() {
        int count = indexingService.reindexAll();
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "indexed", count,
                "message", "Re-indexing completed"
        )));
    }
}
