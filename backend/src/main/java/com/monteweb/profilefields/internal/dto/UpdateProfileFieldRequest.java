package com.monteweb.profilefields.internal.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateProfileFieldRequest(
        @Size(max = 200) String labelDe,
        @Size(max = 200) String labelEn,
        List<String> options,
        Boolean required,
        Integer position,
        Boolean active
) {}
