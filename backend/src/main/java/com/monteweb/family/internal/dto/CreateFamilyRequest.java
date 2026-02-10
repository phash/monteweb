package com.monteweb.family.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFamilyRequest(
        @NotBlank @Size(max = 200) String name
) {
}
