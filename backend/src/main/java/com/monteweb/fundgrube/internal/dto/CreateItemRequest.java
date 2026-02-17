package com.monteweb.fundgrube.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateItemRequest(
        @NotBlank @Size(max = 300) String title,
        @Size(max = 2000) String description,
        UUID sectionId
) {}
