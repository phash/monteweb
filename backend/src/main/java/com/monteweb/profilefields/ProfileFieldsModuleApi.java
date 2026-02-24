package com.monteweb.profilefields;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Public API: Facade interface for the profilefields module.
 */
public interface ProfileFieldsModuleApi {

    /**
     * Returns all active field definitions.
     */
    List<ProfileFieldInfo> getFieldDefinitions();

    /**
     * Returns all custom field values for a given user as fieldId -> value map.
     */
    Map<String, String> getUserFieldValues(UUID userId);

    /**
     * DSGVO: Export all profile-field-related data for a user.
     */
    Map<String, Object> exportUserData(UUID userId);
}
