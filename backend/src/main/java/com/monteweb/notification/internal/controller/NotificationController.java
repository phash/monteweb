package com.monteweb.notification.internal.controller;

import com.monteweb.notification.NotificationInfo;
import com.monteweb.notification.internal.service.NotificationService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.dto.PageResponse;
import com.monteweb.shared.util.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationInfo>>> getNotifications(
            @PageableDefault(size = 20) Pageable pageable) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var page = notificationService.findByUser(userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", count)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        notificationService.deleteNotification(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
