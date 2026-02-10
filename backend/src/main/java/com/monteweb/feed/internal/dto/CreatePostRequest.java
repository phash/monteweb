package com.monteweb.feed.internal.dto;

import com.monteweb.feed.SourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreatePostRequest(
        String title,
        @NotBlank String content,
        @NotNull SourceType sourceType,
        UUID sourceId,
        boolean parentOnly
) {
}
