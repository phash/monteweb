package com.monteweb.family.internal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "family_invitations")
@Getter
@Setter
@NoArgsConstructor
public class FamilyInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "inviter_id", nullable = false)
    private UUID inviterId;

    @Column(name = "invitee_id", nullable = false)
    private UUID inviteeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FamilyMemberRole role = FamilyMemberRole.PARENT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FamilyInvitationStatus status = FamilyInvitationStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public FamilyInvitation(UUID familyId, UUID inviterId, UUID inviteeId, FamilyMemberRole role) {
        this.familyId = familyId;
        this.inviterId = inviterId;
        this.inviteeId = inviteeId;
        this.role = role;
    }
}
