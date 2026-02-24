package com.monteweb.calendar.internal.controller;

import com.monteweb.calendar.internal.model.ICalEvent;
import com.monteweb.calendar.internal.model.ICalSubscription;
import com.monteweb.calendar.internal.service.ICalImportService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.util.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/calendar/ical")
@ConditionalOnProperty(prefix = "monteweb.modules", name = "calendar.enabled", havingValue = "true")
public class ICalController {

    private final ICalImportService iCalImportService;

    public ICalController(ICalImportService iCalImportService) {
        this.iCalImportService = iCalImportService;
    }

    @GetMapping("/subscriptions")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<List<ICalSubscriptionResponse>>> getSubscriptions() {
        var subs = iCalImportService.getAllSubscriptions();
        var response = subs.stream().map(ICalSubscriptionResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/subscriptions")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<ICalSubscriptionResponse>> createSubscription(
            @Valid @RequestBody CreateICalSubscriptionRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var sub = iCalImportService.createSubscription(request.name(), request.url(), request.color(), userId);
        return ResponseEntity.ok(ApiResponse.ok(ICalSubscriptionResponse.from(sub)));
    }

    @DeleteMapping("/subscriptions/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSubscription(@PathVariable UUID id) {
        iCalImportService.deleteSubscription(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/subscriptions/{id}/sync")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<Void>> syncSubscription(@PathVariable UUID id) {
        iCalImportService.syncSubscription(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<ICalEventResponse>>> getICalEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var events = iCalImportService.getImportedEvents(from, to);
        var response = events.stream().map(ICalEventResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // --- DTOs ---

    public record CreateICalSubscriptionRequest(
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 1000) String url,
            @Size(max = 7) String color
    ) {}

    public record ICalSubscriptionResponse(
            UUID id,
            String name,
            String url,
            String color,
            String lastSyncedAt,
            boolean active,
            String createdAt
    ) {
        static ICalSubscriptionResponse from(ICalSubscription sub) {
            return new ICalSubscriptionResponse(
                    sub.getId(),
                    sub.getName(),
                    sub.getUrl(),
                    sub.getColor(),
                    sub.getLastSyncedAt() != null ? sub.getLastSyncedAt().toString() : null,
                    sub.isActive(),
                    sub.getCreatedAt() != null ? sub.getCreatedAt().toString() : null
            );
        }
    }

    public record ICalEventResponse(
            UUID id,
            UUID subscriptionId,
            String title,
            String description,
            String location,
            LocalDate startDate,
            LocalDate endDate,
            String startTime,
            String endTime,
            boolean allDay
    ) {
        static ICalEventResponse from(ICalEvent event) {
            return new ICalEventResponse(
                    event.getId(),
                    event.getSubscriptionId(),
                    event.getTitle(),
                    event.getDescription(),
                    event.getLocation(),
                    event.getStartDate(),
                    event.getEndDate(),
                    event.getStartTime(),
                    event.getEndTime(),
                    event.isAllDay()
            );
        }
    }
}
