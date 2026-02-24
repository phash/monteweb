package com.monteweb.profilefields.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateProfileFieldRequest(
        @NotBlank @Size(max = 50) @Pattern(regexp = "^[a-z][a-z0-9_]*$") String fieldKey,
        @NotBlank @Size(max = 200) String labelDe,
        @NotBlank @Size(max = 200) String labelEn,
        @NotBlank @Pattern(regexp = "^(TEXT|DATE|SELECT|BOOLEAN)$") String fieldType,
        List<String> options,
        boolean required,
        int position
) {}
