package com.monteweb.room.internal.model;

/**
 * Controls discussion behavior in a room.
 */
public enum DiscussionMode {
    /** Full discussions: threads and replies allowed */
    FULL,
    /** Announcements only: only LEADERs/TEACHERs create threads, no replies allowed */
    ANNOUNCEMENTS_ONLY,
    /** Discussions completely disabled */
    DISABLED
}
