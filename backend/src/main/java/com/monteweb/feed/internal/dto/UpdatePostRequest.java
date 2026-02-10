package com.monteweb.feed.internal.dto;

public record UpdatePostRequest(
        String title,
        String content,
        Boolean parentOnly
) {
}
