package com.monteweb.fotobox.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateThreadRequest(
        @NotBlank @Size(max = 300) String title,
        @Size(max = 2000) String description,
        String audience
) {}
