package com.monteweb.feed.internal.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentRequest(
        @NotBlank String content
) {
}
