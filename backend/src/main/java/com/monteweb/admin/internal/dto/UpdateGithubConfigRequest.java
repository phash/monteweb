package com.monteweb.admin.internal.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateGithubConfigRequest(
    @NotBlank String githubRepo,
    @NotBlank String githubPat
) {}
