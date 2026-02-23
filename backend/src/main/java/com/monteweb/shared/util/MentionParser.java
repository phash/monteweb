package com.monteweb.shared.util;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing @mentions in content text.
 * Mention format: @[userId:displayName] e.g. @[550e8400-e29b-41d4-a716-446655440000:Max Mustermann]
 */
public final class MentionParser {

    private static final Pattern MENTION_PATTERN = Pattern.compile(
            "@\\[([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}):([^\\]]+)]"
    );

    private MentionParser() {
        // utility class
    }

    /**
     * Extracts all mentioned user IDs from the given content text.
     *
     * @param content the text that may contain mentions in format @[userId:displayName]
     * @return a set of mentioned user UUIDs, empty set if no mentions or content is null
     */
    public static Set<UUID> extractMentionedUserIds(String content) {
        if (content == null || content.isBlank()) {
            return Set.of();
        }

        Set<UUID> mentionedIds = new HashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            try {
                mentionedIds.add(UUID.fromString(matcher.group(1)));
            } catch (IllegalArgumentException e) {
                // Invalid UUID — skip
            }
        }
        return mentionedIds;
    }
}
