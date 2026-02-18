package com.monteweb.family.internal.controller;

import com.monteweb.calendar.CalendarModuleApi;
import com.monteweb.calendar.EventInfo;
import com.monteweb.family.FamilyInfo;
import com.monteweb.family.internal.dto.*;
import com.monteweb.family.internal.service.FamilyService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.util.AvatarUtils;
import com.monteweb.shared.util.ICalService;
import com.monteweb.shared.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/families")
public class FamilyController {

    private final FamilyService familyService;
    private final CalendarModuleApi calendarModuleApi;
    private final ICalService iCalService;

    public FamilyController(FamilyService familyService,
                            @Autowired(required = false) CalendarModuleApi calendarModuleApi,
                            @Autowired(required = false) ICalService iCalService) {
        this.familyService = familyService;
        this.calendarModuleApi = calendarModuleApi;
        this.iCalService = iCalService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'SECTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<FamilyInfo>>> getAll() {
        var families = familyService.findAll();
        return ResponseEntity.ok(ApiResponse.ok(families));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FamilyInfo>> create(@Valid @RequestBody CreateFamilyRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var family = familyService.create(request.name(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(family));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'SECTION_ADMIN')")
    public ResponseEntity<ApiResponse<FamilyInfo>> updateFamily(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFamilyRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var family = familyService.updateFamily(id, request.name(), userId);
        return ResponseEntity.ok(ApiResponse.ok(family));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'SECTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteFamily(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        familyService.deleteFamily(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Family deleted"));
    }

    @PutMapping("/{id}/active")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'SECTION_ADMIN')")
    public ResponseEntity<ApiResponse<FamilyInfo>> setActive(
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> body) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        boolean active = body.getOrDefault("active", true);
        var family = familyService.setActive(id, active, userId);
        return ResponseEntity.ok(ApiResponse.ok(family));
    }

    @PutMapping("/{id}/hours-exempt")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'SECTION_ADMIN')")
    public ResponseEntity<ApiResponse<FamilyInfo>> setHoursExempt(
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> body) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        boolean exempt = body.getOrDefault("exempt", false);
        var family = familyService.setHoursExempt(id, exempt, userId);
        return ResponseEntity.ok(ApiResponse.ok(family));
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'SECTION_ADMIN')")
    public ResponseEntity<ApiResponse<FamilyInfo>> addMember(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String memberUserId = body.get("userId");
        String role = body.getOrDefault("role", "PARENT");
        familyService.adminAddMember(id, UUID.fromString(memberUserId), role);
        var family = familyService.findById(id).orElseThrow();
        return ResponseEntity.ok(ApiResponse.ok(family));
    }

    @DeleteMapping("/{id}/members/{memberId}/admin")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'SECTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> adminRemoveMember(
            @PathVariable UUID id,
            @PathVariable UUID memberId) {
        familyService.adminRemoveMember(id, memberId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Member removed"));
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

    @PostMapping("/{id}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveFamily(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        familyService.leaveFamily(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Left family"));
    }

    @PostMapping("/{id}/avatar")
    public ResponseEntity<ApiResponse<Void>> uploadAvatar(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        if (!familyService.isUserInFamily(userId, id)) {
            throw new BusinessException("Not a member of this family");
        }
        String dataUrl = AvatarUtils.validateAndConvert(file);
        familyService.updateAvatarUrl(id, dataUrl);
        return ResponseEntity.ok(ApiResponse.ok(null, "Avatar uploaded"));
    }

    @DeleteMapping("/{id}/avatar")
    public ResponseEntity<ApiResponse<Void>> removeAvatar(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        if (!familyService.isUserInFamily(userId, id)) {
            throw new BusinessException("Not a member of this family");
        }
        familyService.updateAvatarUrl(id, null);
        return ResponseEntity.ok(ApiResponse.ok(null, "Avatar removed"));
    }

    // ── Invitations ──────────────────────────────────────────────────────

    @PostMapping("/{id}/invitations")
    public ResponseEntity<ApiResponse<FamilyInvitationInfo>> inviteMember(
            @PathVariable UUID id,
            @Valid @RequestBody InviteMemberRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var invitation = familyService.inviteMember(id, request.inviteeId(), request.role(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(invitation));
    }

    @GetMapping("/my-invitations")
    public ResponseEntity<ApiResponse<List<FamilyInvitationInfo>>> getMyInvitations() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(familyService.getMyPendingInvitations(userId)));
    }

    @PostMapping("/invitations/{id}/accept")
    public ResponseEntity<ApiResponse<FamilyInvitationInfo>> acceptInvitation(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(familyService.acceptInvitation(id, userId)));
    }

    @PostMapping("/invitations/{id}/decline")
    public ResponseEntity<ApiResponse<FamilyInvitationInfo>> declineInvitation(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(familyService.declineInvitation(id, userId)));
    }

    @GetMapping("/{id}/invitations")
    public ResponseEntity<ApiResponse<List<FamilyInvitationInfo>>> getFamilyInvitations(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        if (!familyService.isUserInFamily(userId, id)) {
            throw new BusinessException("Not a member of this family");
        }
        return ResponseEntity.ok(ApiResponse.ok(familyService.getFamilyInvitations(id)));
    }

    // -- Family Calendar -------------------------------------------------------

    @GetMapping("/{id}/calendar")
    public ResponseEntity<ApiResponse<List<EventInfo>>> getFamilyCalendar(
            @PathVariable UUID id,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        if (!familyService.isUserInFamily(userId, id)) {
            throw new BusinessException("Not a member of this family");
        }
        if (calendarModuleApi == null) {
            return ResponseEntity.ok(ApiResponse.ok(List.of()));
        }
        var memberUserIds = getFamilyMemberUserIds(id);
        var events = calendarModuleApi.getEventsForUserIds(memberUserIds, from, to);
        return ResponseEntity.ok(ApiResponse.ok(events));
    }

    @GetMapping("/{id}/calendar/ical")
    public ResponseEntity<byte[]> getFamilyCalendarIcal(
            @PathVariable UUID id,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        if (!familyService.isUserInFamily(userId, id)) {
            throw new BusinessException("Not a member of this family");
        }
        if (calendarModuleApi == null || iCalService == null) {
            String empty = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//MonteWeb//Calendar//DE\r\nEND:VCALENDAR\r\n";
            return buildIcalResponse(empty.getBytes(StandardCharsets.UTF_8));
        }

        LocalDate effectiveFrom = from != null ? from : LocalDate.now();
        LocalDate effectiveTo = to != null ? to : LocalDate.now().plusDays(365);

        var memberUserIds = getFamilyMemberUserIds(id);
        var events = calendarModuleApi.getEventsForUserIds(memberUserIds, effectiveFrom, effectiveTo);
        String icalContent = iCalService.generateIcal(events);
        return buildIcalResponse(icalContent.getBytes(StandardCharsets.UTF_8));
    }

    private List<UUID> getFamilyMemberUserIds(UUID familyId) {
        return familyService.findById(familyId)
                .map(f -> f.members().stream().map(FamilyInfo.FamilyMemberInfo::userId).toList())
                .orElse(List.of());
    }

    private ResponseEntity<byte[]> buildIcalResponse(byte[] content) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/calendar"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=family-calendar.ics");
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }
}
