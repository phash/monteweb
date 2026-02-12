package com.monteweb.room;

/**
 * Defines how users can join a room.
 */
public enum JoinPolicy {
    /** Anyone can join directly */
    OPEN,
    /** Users must send a join request that a LEADER approves */
    REQUEST,
    /** Users can only join via invitation by a LEADER */
    INVITE_ONLY
}
