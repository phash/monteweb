package com.monteweb.feed.internal.dto;

import com.monteweb.feed.SourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreatePostRequest(
        @Size(max = 500) String title,
        @Size(max = 50000) String content,
        @NotNull SourceType sourceType,
        UUID sourceId,
        boolean parentOnly,
        @Valid CreatePollRequest poll
) {
}
