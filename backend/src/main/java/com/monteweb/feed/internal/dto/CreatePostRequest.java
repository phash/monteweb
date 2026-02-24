package com.monteweb.feed.internal.dto;

import com.monteweb.feed.SourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreatePostRequest(
        String title,
        String content,
        @NotNull SourceType sourceType,
        UUID sourceId,
        boolean parentOnly,
        @Valid CreatePollRequest poll
) {
}
