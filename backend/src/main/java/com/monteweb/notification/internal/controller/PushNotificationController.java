package com.monteweb.notification.internal.controller;

import com.monteweb.notification.internal.service.WebPushService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.util.SecurityUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications/push")
@ConditionalOnProperty(prefix = "monteweb.push", name = "enabled", havingValue = "true")
public class PushNotificationController {

    private final WebPushService webPushService;

    public PushNotificationController(WebPushService webPushService) {
        this.webPushService = webPushService;
    }

    @GetMapping("/public-key")
    public ResponseEntity<ApiResponse<Map<String, String>>> getPublicKey() {
        return ResponseEntity.ok(ApiResponse.ok(
                Map.of("publicKey", webPushService.getPublicKey())));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse<Void>> subscribe(@RequestBody SubscribeRequest request) {
        var userId = SecurityUtils.requireCurrentUserId();
        webPushService.subscribe(userId, request.endpoint(), request.p256dh(), request.auth());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<ApiResponse<Void>> unsubscribe(@RequestBody UnsubscribeRequest request) {
        var userId = SecurityUtils.requireCurrentUserId();
        webPushService.unsubscribe(userId, request.endpoint());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    public record SubscribeRequest(String endpoint, String p256dh, String auth) {}
    public record UnsubscribeRequest(String endpoint) {}
}
