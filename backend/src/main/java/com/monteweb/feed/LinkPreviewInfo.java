package com.monteweb.feed;

/**
 * Public DTO: Link preview metadata extracted from OpenGraph tags.
 */
public record LinkPreviewInfo(
        String url,
        String title,
        String description,
        String imageUrl,
        String siteName
) {
}
