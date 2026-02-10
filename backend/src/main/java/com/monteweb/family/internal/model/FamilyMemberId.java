package com.monteweb.family.internal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FamilyMemberId implements Serializable {

    @Column(name = "family_id")
    private UUID familyId;

    @Column(name = "user_id")
    private UUID userId;
}
