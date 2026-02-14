package com.monteweb.cleaning;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Public API: Facade interface for the cleaning module.
 * Other modules interact with cleaning exclusively through this interface.
 */
public interface CleaningModuleApi {

    List<CleaningSlotInfo> getUpcomingSlotsForSection(UUID sectionId, int limit);

    BigDecimal getCleaningHoursForFamily(UUID familyId);

    /**
     * Returns slots needing more registrations (for feed banners).
     */
    List<CleaningSlotInfo> getSlotsNeedingParticipants(LocalDate from, LocalDate to);

    /**
     * Returns cleaning hours for a family within a specific date range.
     */
    BigDecimal getCleaningHoursForFamilyInRange(UUID familyId, LocalDate from, LocalDate to);

    /**
     * Links a job ID back to a cleaning config (called by jobboard after creating the job).
     */
    void linkJobToConfig(UUID configId, UUID jobId);
}
