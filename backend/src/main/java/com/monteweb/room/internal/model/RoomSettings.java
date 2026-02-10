package com.monteweb.room.internal.model;

public record RoomSettings(
        boolean chatEnabled,
        boolean filesEnabled,
        boolean parentSpaceEnabled,
        String visibility // MEMBERS_ONLY, SECTION, ALL
) {
    public static RoomSettings defaults() {
        return new RoomSettings(false, false, false, "MEMBERS_ONLY");
    }
}
