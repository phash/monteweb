package com.monteweb.family;

import java.util.List;
import java.util.UUID;

/**
 * Public API: Read-only family information for cross-module use.
 */
public record FamilyInfo(
        UUID id,
        String name,
        String avatarUrl,
        List<FamilyMemberInfo> members
) {
    public record FamilyMemberInfo(
            UUID userId,
            String displayName,
            String role
    ) {
    }
}
