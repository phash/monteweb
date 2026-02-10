package com.monteweb.school.internal.controller;

import com.monteweb.school.SchoolSectionInfo;
import com.monteweb.school.internal.dto.CreateSectionRequest;
import com.monteweb.school.internal.dto.UpdateSectionRequest;
import com.monteweb.school.internal.service.SchoolSectionService;
import com.monteweb.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sections")
public class SchoolSectionController {

    private final SchoolSectionService service;

    public SchoolSectionController(SchoolSectionService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SchoolSectionInfo>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(service.findAllActive()));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<SchoolSectionInfo>> create(@Valid @RequestBody CreateSectionRequest request) {
        var section = service.create(request.name(), request.description(), request.sortOrder());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(section));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<SchoolSectionInfo>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSectionRequest request) {
        var section = service.update(id, request.name(), request.description(), request.sortOrder());
        return ResponseEntity.ok(ApiResponse.ok(section));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        service.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Section deactivated"));
    }
}
