package com.monteweb.family.internal.repository;

import com.monteweb.family.internal.model.Family;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FamilyRepository extends JpaRepository<Family, UUID> {

    Optional<Family> findByInviteCode(String inviteCode);

    @Query("SELECT f FROM Family f JOIN f.members m WHERE m.userId = :userId")
    List<Family> findByMemberUserId(UUID userId);

    @Query("SELECT COUNT(m) > 0 FROM FamilyMember m WHERE m.family.id = :familyId AND m.userId = :userId")
    boolean isMember(UUID userId, UUID familyId);

    Optional<Family> findByNameIgnoreCase(String name);
}
