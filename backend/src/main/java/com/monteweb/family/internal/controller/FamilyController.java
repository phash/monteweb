package com.monteweb.family.internal.controller;

import com.monteweb.family.FamilyInfo;
import com.monteweb.family.internal.dto.AddChildRequest;
import com.monteweb.family.internal.dto.CreateFamilyRequest;
import com.monteweb.family.internal.dto.JoinFamilyRequest;
import com.monteweb.family.internal.service.FamilyService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/families")
public class FamilyController {

    private final FamilyService familyService;

    public FamilyController(FamilyService familyService) {
        this.familyService = familyService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FamilyInfo>> create(@Valid @RequestBody CreateFamilyRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var family = familyService.create(request.name(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(family));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<FamilyInfo>>> getMyFamilies() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var families = familyService.findByUserId(userId);
        return ResponseEntity.ok(ApiResponse.ok(families));
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateInviteCode(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        String code = familyService.generateInviteCode(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("inviteCode", code)));
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<FamilyInfo>> joinByInviteCode(@Valid @RequestBody JoinFamilyRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var family = familyService.joinByInviteCode(request.inviteCode(), userId);
        return ResponseEntity.ok(ApiResponse.ok(family));
    }

    @PostMapping("/{id}/children")
    public ResponseEntity<ApiResponse<FamilyInfo>> addChild(
            @PathVariable UUID id,
            @Valid @RequestBody AddChildRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var family = familyService.addChild(id, request.childUserId(), userId);
        return ResponseEntity.ok(ApiResponse.ok(family));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID memberId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        familyService.removeMember(id, memberId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Member removed"));
    }
}
