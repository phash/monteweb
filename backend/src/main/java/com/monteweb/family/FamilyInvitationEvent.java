package com.monteweb.family;

import java.util.UUID;

/**
 * Public API: Event published when a family invitation is created or resolved.
 */
public record FamilyInvitationEvent(
        UUID invitationId,
        UUID familyId,
        String familyName,
        UUID inviterId,
        String inviterName,
        UUID inviteeId,
        boolean accepted
) {
}
