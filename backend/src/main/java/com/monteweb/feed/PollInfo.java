package com.monteweb.feed;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Public API: Inline poll data attached to a feed post.
 * Also reused by the messaging module for message polls.
 */
public record PollInfo(
        UUID id,
        String question,
        boolean multiple,
        boolean closed,
        int totalVotes,
        List<OptionInfo> options,
        Instant closesAt
) {
    public record OptionInfo(
            UUID id,
            String label,
            int voteCount,
            boolean userVoted
    ) {
    }
}
