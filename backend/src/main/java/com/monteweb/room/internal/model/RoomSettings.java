package com.monteweb.room.internal.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RoomSettings(
        boolean chatEnabled,
        boolean filesEnabled,
        boolean parentSpaceEnabled,
        String visibility, // MEMBERS_ONLY, SECTION, ALL
        String discussionMode, // FULL, ANNOUNCEMENTS_ONLY, DISABLED (default: FULL)
        boolean allowMemberThreadCreation, // PARENTs can create threads when true
        boolean childDiscussionEnabled // STUDENTs can see/reply to KINDER threads when true
) {
    public static RoomSettings defaults() {
        return new RoomSettings(false, false, false, "MEMBERS_ONLY", "FULL", false, false);
    }

    /**
     * Returns the effective discussion mode, defaulting to FULL for backwards compatibility.
     */
    public DiscussionMode effectiveDiscussionMode() {
        if (discussionMode == null || discussionMode.isBlank()) {
            return DiscussionMode.FULL;
        }
        return DiscussionMode.valueOf(discussionMode);
    }
}
