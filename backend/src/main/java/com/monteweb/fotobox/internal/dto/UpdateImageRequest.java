package com.monteweb.fotobox.internal.dto;

import jakarta.validation.constraints.Size;

public record UpdateImageRequest(
        @Size(max = 500) String caption,
        Integer sortOrder
) {}
