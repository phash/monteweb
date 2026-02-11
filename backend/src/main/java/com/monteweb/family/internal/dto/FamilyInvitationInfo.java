package com.monteweb.family.internal.dto;

import java.time.Instant;
import java.util.UUID;

public record FamilyInvitationInfo(
        UUID id,
        UUID familyId,
        String familyName,
        UUID inviterId,
        String inviterName,
        UUID inviteeId,
        String inviteeName,
        String role,
        String status,
        Instant createdAt,
        Instant resolvedAt
) {
}
