package com.monteweb.fotobox.internal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateSettingsRequest(
        Boolean enabled,
        String defaultPermission,
        Integer maxImagesPerThread,
        @Min(1) @Max(50) Integer maxFileSizeMb
) {}
