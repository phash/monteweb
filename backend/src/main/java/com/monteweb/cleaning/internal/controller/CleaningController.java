package com.monteweb.cleaning.internal.controller;

import com.monteweb.cleaning.CleaningSlotInfo;
import com.monteweb.cleaning.internal.service.CleaningService;
import com.monteweb.family.FamilyInfo;
import com.monteweb.family.FamilyModuleApi;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.dto.PageResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.exception.ForbiddenException;
import com.monteweb.shared.util.SecurityUtils;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<ApiResponse<PageResponse<CleaningSlotInfo>>> getUpcomingSlots(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(cleaningService.getUpcomingSlots(pageable))));
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
        // SUPERADMIN, SECTION_ADMIN, TEACHER do not perform cleaning hours
        if (user.role() == com.monteweb.user.UserRole.SUPERADMIN
                || user.role() == com.monteweb.user.UserRole.SECTION_ADMIN
                || user.role() == com.monteweb.user.UserRole.TEACHER) {
            throw new BusinessException("This role does not perform cleaning hours");
        }
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

    /**
     * Section-wide (or, with no sectionId, global) list of upcoming slots that have an
     * open swap offer, so other parents can discover slots to take over (US-230).
     */
    @GetMapping("/slots/swaps")
    public ResponseEntity<ApiResponse<List<CleaningSlotInfo>>> getOpenSwapSlots(
            @RequestParam(required = false) UUID sectionId) {
        return ResponseEntity.ok(ApiResponse.ok(cleaningService.getOpenSwapSlots(sectionId)));
    }

    /**
     * Take over a slot offered for swap by another participant (US-230 step 4).
     */
    @PostMapping("/slots/{id}/swap/take")
    public ResponseEntity<ApiResponse<CleaningSlotInfo>> takeOverSwap(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        UserInfo user = userModuleApi.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
        // SUPERADMIN, SECTION_ADMIN, TEACHER do not perform cleaning hours
        if (user.role() == UserRole.SUPERADMIN
                || user.role() == UserRole.SECTION_ADMIN
                || user.role() == UserRole.TEACHER) {
            throw new BusinessException("This role does not perform cleaning hours");
        }
        List<FamilyInfo> families = familyModuleApi.findByUserId(userId);
        if (families.isEmpty()) {
            throw new BusinessException("User must belong to a family to take over cleaning");
        }
        UUID familyId = families.get(0).id();
        return ResponseEntity.ok(ApiResponse.ok(
                cleaningService.takeOverSwap(id, userId, user.displayName(), familyId)));
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

    @GetMapping("/registrations/pending-confirmation")
    public ResponseEntity<ApiResponse<List<CleaningSlotInfo.RegistrationInfo>>> getPendingConfirmations() {
        requireCleaningAdmin();
        return ResponseEntity.ok(ApiResponse.ok(cleaningService.getPendingCleaningConfirmations()));
    }

    @PutMapping("/registrations/{registrationId}/confirm")
    public ResponseEntity<ApiResponse<CleaningSlotInfo.RegistrationInfo>> confirmRegistration(
            @PathVariable UUID registrationId) {
        UUID userId = requireCleaningAdmin();
        return ResponseEntity.ok(ApiResponse.ok(cleaningService.confirmCleaningRegistration(registrationId, userId)));
    }

    @PutMapping("/registrations/{registrationId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectRegistration(
            @PathVariable UUID registrationId) {
        requireCleaningAdmin();
        cleaningService.rejectCleaningRegistration(registrationId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PutMapping("/registrations/{registrationId}/update-minutes")
    public ResponseEntity<ApiResponse<CleaningSlotInfo.RegistrationInfo>> updateRegistrationMinutes(
            @PathVariable UUID registrationId,
            @Valid @RequestBody UpdateMinutesRequest request) {
        requireCleaningAdmin();
        var result = cleaningService.updateRegistrationMinutes(registrationId, request.actualMinutes());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    public record UpdateMinutesRequest(@Min(0) @Max(1440) int actualMinutes) {}

    /**
     * Authorizes the current user as a cleaning administrator. Accepts SUPERADMIN and
     * SECTION_ADMIN role holders as well as the PUTZORGA / ELTERNBEIRAT / SECTION_ADMIN
     * special roles (global or section-scoped). These registration-level actions are not
     * tied to a single section, so any cleaning-admin scope is sufficient (US-231).
     *
     * @return the current user's id
     */
    private UUID requireCleaningAdmin() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        UserInfo user = userModuleApi.findById(userId)
                .orElseThrow(() -> new ForbiddenException("User not found"));
        if (user.role() == UserRole.SUPERADMIN || user.role() == UserRole.SECTION_ADMIN) {
            return userId;
        }
        if (user.specialRoles() != null) {
            for (String role : user.specialRoles()) {
                if (role.equals("PUTZORGA") || role.equals("ELTERNBEIRAT")
                        || role.startsWith("PUTZORGA:") || role.startsWith("ELTERNBEIRAT:")
                        || role.startsWith("SECTION_ADMIN:")) {
                    return userId;
                }
            }
        }
        throw new ForbiddenException("Not authorized to confirm cleaning hours");
    }
}
