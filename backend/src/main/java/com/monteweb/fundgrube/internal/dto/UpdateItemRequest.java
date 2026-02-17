package com.monteweb.fundgrube.internal.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateItemRequest(
        @Size(max = 300) String title,
        @Size(max = 2000) String description,
        UUID sectionId
) {}
