package com.monteweb.parentletter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UpdateParentLetterRequest(
    @NotBlank @Size(max = 300) String title,
    @NotBlank String content,
    Instant sendDate,
    Instant deadline,
    Integer reminderDays,
    List<UUID> studentIds
) {}
