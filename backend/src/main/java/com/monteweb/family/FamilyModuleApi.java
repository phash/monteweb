package com.monteweb.family;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API: Facade interface for the family module.
 * Other modules interact with families exclusively through this interface.
 */
public interface FamilyModuleApi {

    Optional<FamilyInfo> findById(UUID familyId);

    /**
     * Returns all families that a user belongs to.
     * A parent belongs to exactly one family; a child may belong to multiple (separated parents).
     */
    List<FamilyInfo> findByUserId(UUID userId);

    boolean isUserInFamily(UUID userId, UUID familyId);
}
