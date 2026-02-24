package com.monteweb.messaging.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record CreateMessagePollRequest(
        @NotBlank @Size(max = 500) String question,
        @NotEmpty @Size(min = 2, max = 10) List<@NotBlank @Size(max = 200) String> options,
        boolean multiple,
        Instant closesAt
) {
}
