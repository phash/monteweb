package com.monteweb.admin.internal.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateErrorStatusRequest(@NotBlank String status) {}
