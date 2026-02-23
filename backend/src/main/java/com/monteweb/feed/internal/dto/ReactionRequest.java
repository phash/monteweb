package com.monteweb.feed.internal.dto;

import jakarta.validation.constraints.NotBlank;

public record ReactionRequest(
        @NotBlank String emoji
) {
}
