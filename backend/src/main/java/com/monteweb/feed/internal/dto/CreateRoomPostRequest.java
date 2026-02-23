package com.monteweb.feed.internal.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRoomPostRequest(
        String title,
        @NotBlank String content
) {
}
