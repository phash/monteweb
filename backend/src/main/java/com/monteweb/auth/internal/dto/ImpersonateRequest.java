package com.monteweb.auth.internal.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ImpersonateRequest(@NotNull UUID targetUserId) {}
