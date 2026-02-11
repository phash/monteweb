package com.monteweb.cleaning.internal.controller;

import com.monteweb.cleaning.CleaningSlotInfo;
import com.monteweb.cleaning.internal.service.CleaningService;
import com.monteweb.family.FamilyInfo;
import com.monteweb.family.FamilyModuleApi;
import com.monteweb.shared.dto.ApiResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.util.SecurityUtils;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cleaning")
@ConditionalOnProperty(prefix = "monteweb.modules.cleaning", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class CleaningController {

    private final CleaningService cleaningService;
    private final UserModuleApi userModuleApi;
    private final FamilyModuleApi familyModuleApi;

    @GetMapping("/slots")
    public ResponseEntity<ApiResponse<Page<CleaningSlotInfo>>> getUpcomingSlots(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(cleaningService.getUpcomingSlots(pageable)));
    }

    @GetMapping("/slots/mine")
    public ResponseEntity<ApiResponse<List<CleaningSlotInfo>>> getMySlots() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(cleaningService.getMySlots(userId)));
    }

    @GetMapping("/slots/{id}")
    public ResponseEntity<ApiResponse<CleaningSlotInfo>> getSlot(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(cleaningService.getSlotById(id)));
    }

    @PostMapping("/slots/{id}/register")
    public ResponseEntity<ApiResponse<CleaningSlotInfo>> registerForSlot(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        UserInfo user = userModuleApi.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
        List<FamilyInfo> families = familyModuleApi.findByUserId(userId);
        if (families.isEmpty()) {
            throw new BusinessException("User must belong to a family to register for cleaning");
        }
        UUID familyId = families.get(0).id();
        String userName = user.displayName();

        return ResponseEntity.ok(ApiResponse.ok(
                cleaningService.registerForSlot(id, userId, userName, familyId)));
    }

    @DeleteMapping("/slots/{id}/register")
    public ResponseEntity<ApiResponse<Void>> unregisterFromSlot(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        cleaningService.unregisterFromSlot(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/slots/{id}/swap")
    public ResponseEntity<ApiResponse<Void>> offerSwap(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        cleaningService.offerSwap(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/slots/{id}/swaps")
    public ResponseEntity<ApiResponse<List<CleaningSlotInfo.RegistrationInfo>>> getSwapOffers(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(cleaningService.getSwapOffers(id)));
    }

    @PostMapping("/slots/{id}/checkin")
    public ResponseEntity<ApiResponse<CleaningSlotInfo>> checkIn(
            @PathVariable UUID id, @RequestBody CheckInRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                cleaningService.checkIn(id, userId, request.qrToken())));
    }

    @PostMapping("/slots/{id}/checkout")
    public ResponseEntity<ApiResponse<CleaningSlotInfo>> checkOut(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(cleaningService.checkOut(id, userId)));
    }

    public record CheckInRequest(String qrToken) {
    }
}
