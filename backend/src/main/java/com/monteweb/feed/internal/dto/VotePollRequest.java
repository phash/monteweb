package com.monteweb.feed.internal.dto;

import java.util.List;
import java.util.UUID;

public record VotePollRequest(
        List<UUID> optionIds
) {
}
