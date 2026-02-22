package com.monteweb.forms;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface FormsModuleApi {

    List<FormInfo> getPublishedFormsForRoom(UUID roomId);

    List<FormInfo> getPublishedFormsForSection(UUID sectionId);

    List<FormInfo> getPublishedSchoolForms();

    Optional<FormInfo> findById(UUID formId, UUID currentUserId);

    boolean hasUserResponded(UUID formId, UUID userId);

    /**
     * DSGVO: Export all forms-related data for a user.
     */
    Map<String, Object> exportUserData(UUID userId);
}
