package com.monteweb.feed.internal.dto;

import java.time.Instant;
import java.util.UUID;

public record SystemBannerResponse(
        UUID id,
        String title,
        String content,
        String bannerType,  // INFO, WARNING, ACTION_REQUIRED
        String link,
        Instant expiresAt
) {
}
