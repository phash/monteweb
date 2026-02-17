package com.monteweb.fundgrube;

import java.util.UUID;

/**
 * Public DTO: image attached to a Fundgrube item.
 */
public record FundgrubeImageInfo(
        UUID id,
        UUID itemId,
        String originalFilename,
        String imageUrl,
        String thumbnailUrl,
        long fileSize,
        String contentType
) {}
