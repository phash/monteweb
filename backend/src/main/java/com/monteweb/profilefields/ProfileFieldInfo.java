package com.monteweb.profilefields;

import java.util.List;
import java.util.UUID;

/**
 * Public DTO: Read-only profile field definition for cross-module use.
 */
public record ProfileFieldInfo(
        UUID id,
        String fieldKey,
        String labelDe,
        String labelEn,
        String fieldType,
        List<String> options,
        boolean required,
        int position
) {}
