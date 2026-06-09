package com.monteweb.feed.internal.dto;

import com.monteweb.feed.SourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreatePostRequest(
        @Size(max = 500) String title,
        @Size(max = 50000) String content,
        @NotNull SourceType sourceType,
        UUID sourceId,
        boolean parentOnly,
        @Valid CreatePollRequest poll,
        /**
         * Optional: restrict this post to the given users only (US-080).
         * When non-empty, only the listed users (plus the author/admins) can read it.
         */
        @Size(max = 500) List<UUID> targetUserIds
) {
}
