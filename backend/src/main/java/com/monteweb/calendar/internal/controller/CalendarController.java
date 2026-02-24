package com.monteweb.calendar.internal.controller;

import com.monteweb.calendar.*;
import com.monteweb.calendar.internal.service.CalendarService;
import com.monteweb.jobboard.JobInfo;
import com.monteweb.jobboard.JobboardModuleApi;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.dto.PageResponse;
import com.monteweb.shared.util.ICalService;
import com.monteweb.shared.util.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/calendar")
@ConditionalOnProperty(prefix = "monteweb.modules", name = "calendar.enabled", havingValue = "true")
public class CalendarController {

    private final CalendarService calendarService;
    private final JobboardModuleApi jobboardModuleApi;
    private final ICalService iCalService;

    public CalendarController(CalendarService calendarService,
                              @Autowired(required = false) JobboardModuleApi jobboardModuleApi,
                              ICalService iCalService) {
        this.calendarService = calendarService;
        this.jobboardModuleApi = jobboardModuleApi;
        this.iCalService = iCalService;
    }

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<PageResponse<EventInfo>>> getEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 50) Pageable pageable) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var page = calendarService.getPersonalEvents(userId, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @PostMapping("/events")
    public ResponseEntity<ApiResponse<EventInfo>> createEvent(
            @Valid @RequestBody CreateEventRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var event = calendarService.createEvent(request, userId);
        return ResponseEntity.ok(ApiResponse.ok(event));
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<ApiResponse<EventInfo>> getEvent(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var event = calendarService.getEvent(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(event));
    }

    @PutMapping("/events/{id}")
    public ResponseEntity<ApiResponse<EventInfo>> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEventRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var event = calendarService.updateEvent(id, request, userId);
        return ResponseEntity.ok(ApiResponse.ok(event));
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        calendarService.deleteEvent(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/events/{id}/cancel")
    public ResponseEntity<ApiResponse<EventInfo>> cancelEvent(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var event = calendarService.cancelEvent(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(event));
    }

    @PostMapping("/events/{id}/rsvp")
    public ResponseEntity<ApiResponse<EventInfo>> rsvp(
            @PathVariable UUID id,
            @Valid @RequestBody RsvpRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var event = calendarService.rsvp(id, userId, request.status());
        return ResponseEntity.ok(ApiResponse.ok(event));
    }

    @PostMapping("/events/{id}/jitsi")
    public ResponseEntity<ApiResponse<EventInfo>> generateJitsiRoom(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var event = calendarService.generateJitsiRoom(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(event));
    }

    @DeleteMapping("/events/{id}/jitsi")
    public ResponseEntity<ApiResponse<EventInfo>> removeJitsiRoom(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var event = calendarService.removeJitsiRoom(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(event));
    }

    @GetMapping("/rooms/{roomId}/events")
    public ResponseEntity<ApiResponse<PageResponse<EventInfo>>> getRoomEvents(
            @PathVariable UUID roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 50) Pageable pageable) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        LocalDate effectiveFrom = from != null ? from : LocalDate.now();
        LocalDate effectiveTo = to != null ? to : effectiveFrom.plusMonths(3);
        var page = calendarService.getRoomEvents(roomId, userId, effectiveFrom, effectiveTo, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @GetMapping("/events/{eventId}/jobs")
    public ResponseEntity<ApiResponse<List<JobInfo>>> getEventJobs(@PathVariable UUID eventId) {
        if (jobboardModuleApi == null) {
            return ResponseEntity.ok(ApiResponse.ok(List.of()));
        }
        return ResponseEntity.ok(ApiResponse.ok(jobboardModuleApi.getJobsForEvent(eventId)));
    }

    @GetMapping("/events/{id}/export")
    public ResponseEntity<byte[]> exportEvent(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var event = calendarService.getEvent(id, userId);
        String ical = iCalService.generateIcal(List.of(event));
        byte[] bytes = ical.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"event.ics\"")
                .contentType(MediaType.parseMediaType("text/calendar"))
                .contentLength(bytes.length)
                .body(bytes);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var page = calendarService.getPersonalEvents(userId, from, to, Pageable.unpaged());
        String ical = iCalService.generateIcal(page.getContent());
        byte[] bytes = ical.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"calendar.ics\"")
                .contentType(MediaType.parseMediaType("text/calendar"))
                .contentLength(bytes.length)
                .body(bytes);
    }

    public record RsvpRequest(@NotNull RsvpStatus status) {
    }
}
