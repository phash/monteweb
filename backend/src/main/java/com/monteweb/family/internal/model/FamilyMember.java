package com.monteweb.family.internal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "family_members")
@Getter
@Setter
@NoArgsConstructor
public class FamilyMember {

    @EmbeddedId
    private FamilyMemberId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("familyId")
    @JoinColumn(name = "family_id")
    private Family family;

    @Column(name = "user_id", insertable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FamilyMemberRole role;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();

    public FamilyMember(Family family, UUID userId, FamilyMemberRole role) {
        this.id = new FamilyMemberId(family.getId(), userId);
        this.family = family;
        this.userId = userId;
        this.role = role;
        this.joinedAt = Instant.now();
    }
}
