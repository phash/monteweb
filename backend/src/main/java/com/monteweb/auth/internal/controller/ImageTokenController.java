package com.monteweb.auth.internal.controller;

import com.monteweb.auth.internal.service.JwtService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.util.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Generates short-lived image tokens for authenticated image access.
 * These tokens replace full JWT tokens in image URL query parameters,
 * reducing the risk of token leakage via logs, referrer headers, etc.
 */
@RestController
@RequestMapping("/api/v1/image-token")
public class ImageTokenController {

    private final JwtService jwtService;

    public ImageTokenController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> getImageToken() {
        var userId = SecurityUtils.requireCurrentUserId();
        String token = jwtService.generateImageToken(userId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("token", token)));
    }
}
