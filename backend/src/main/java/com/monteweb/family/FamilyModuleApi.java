package com.monteweb.family;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API: Facade interface for the family module.
 * Other modules interact with families exclusively through this interface.
 */
public interface FamilyModuleApi {

    Optional<FamilyInfo> findById(UUID familyId);

    /**
     * Returns all families in the system.
     */
    List<FamilyInfo> findAll();

    /**
     * Returns all families that a user belongs to.
     * A parent belongs to exactly one family; a child may belong to multiple (separated parents).
     */
    List<FamilyInfo> findByUserId(UUID userId);

    boolean isUserInFamily(UUID userId, UUID familyId);

    /**
     * Admin: Add a user to a family without auth checks.
     */
    void adminAddMember(UUID familyId, UUID userId, String role);

    /**
     * Admin: Remove a user from a family without auth checks.
     */
    void adminRemoveMember(UUID familyId, UUID userId);

    /**
     * DSGVO: Export all family-related data for a user.
     */
    Map<String, Object> exportUserData(UUID userId);

    /**
     * Find a family by name (case-insensitive). Used by CSV import.
     */
    Optional<FamilyInfo> findByNameIgnoreCase(String name);

    /**
     * Admin: Create a family with a given name (no creator user added). Used by CSV import.
     */
    FamilyInfo adminCreateFamily(String name);
}
