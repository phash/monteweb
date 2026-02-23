package com.monteweb.wiki.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreatePageRequest(
        @NotBlank @Size(max = 255) String title,
        String content,
        UUID parentId
) {
}
