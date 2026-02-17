package com.monteweb.fundgrube;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Public DTO: Read-only item info for cross-module use.
 */
public record FundgrubeItemInfo(
        UUID id,
        String title,
        String description,
        UUID sectionId,
        String sectionName,
        UUID createdBy,
        String createdByName,
        Instant createdAt,
        Instant updatedAt,
        UUID claimedBy,
        String claimedByName,
        Instant claimedAt,
        Instant expiresAt,
        boolean claimed,
        List<FundgrubeImageInfo> images
) {}
