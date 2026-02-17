package com.monteweb.fundgrube.internal.dto;

import jakarta.validation.constraints.Size;

public record ClaimItemRequest(
        @Size(max = 1000) String comment
) {}
