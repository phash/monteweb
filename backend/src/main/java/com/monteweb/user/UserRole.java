package com.monteweb.user;

/**
 * Public API: User roles available system-wide.
 */
public enum UserRole {
    SUPERADMIN,
    SECTION_ADMIN,
    TEACHER,
    PARENT,
    STUDENT;

    public static UserRole fromStringOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
