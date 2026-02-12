package com.monteweb.fotobox.internal.dto;

import jakarta.validation.constraints.Size;

public record UpdateThreadRequest(
        @Size(max = 300) String title,
        @Size(max = 2000) String description
) {}
