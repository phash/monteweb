package com.monteweb.feed.internal.dto;

import jakarta.validation.constraints.Size;

public record UpdatePostRequest(
        @Size(max = 500) String title,
        @Size(max = 50000) String content,
        Boolean parentOnly
) {
}
