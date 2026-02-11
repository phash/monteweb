package com.monteweb.family.internal.repository;

import com.monteweb.family.internal.model.FamilyInvitation;
import com.monteweb.family.internal.model.FamilyInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FamilyInvitationRepository extends JpaRepository<FamilyInvitation, UUID> {

    List<FamilyInvitation> findByInviteeIdAndStatusOrderByCreatedAtDesc(UUID inviteeId, FamilyInvitationStatus status);

    List<FamilyInvitation> findByFamilyIdAndStatusOrderByCreatedAtDesc(UUID familyId, FamilyInvitationStatus status);

    boolean existsByFamilyIdAndInviteeIdAndStatus(UUID familyId, UUID inviteeId, FamilyInvitationStatus status);
}
