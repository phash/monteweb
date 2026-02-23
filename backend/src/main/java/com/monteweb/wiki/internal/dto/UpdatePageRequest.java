package com.monteweb.wiki.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePageRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String content
) {
}
