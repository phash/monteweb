package com.monteweb.room.internal.dto;

import com.monteweb.room.RoomRole;
import com.monteweb.room.internal.model.RoomSettings;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoomDetailResponse(
        UUID id,
        String name,
        String description,
        String publicDescription,
        String avatarUrl,
        String type,
        UUID sectionId,
        RoomSettings settings,
        boolean archived,
        UUID createdBy,
        Instant createdAt,
        List<MemberResponse> members
) {
    public record MemberResponse(
            UUID userId,
            String displayName,
            String avatarUrl,
            RoomRole role,
            Instant joinedAt
    ) {
    }
}
