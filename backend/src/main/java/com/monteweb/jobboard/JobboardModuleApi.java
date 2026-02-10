package com.monteweb.jobboard;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API: Facade interface for the jobboard module.
 * Other modules interact with the job board exclusively through this interface.
 */
public interface JobboardModuleApi {

    Optional<FamilyHoursInfo> getFamilyHours(UUID familyId);

    BigDecimal getConfirmedHoursForFamily(UUID familyId);
}
