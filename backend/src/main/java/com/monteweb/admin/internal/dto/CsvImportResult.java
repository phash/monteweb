package com.monteweb.admin.internal.dto;

import java.util.List;

public record CsvImportResult(
        int totalRows,
        int usersCreated,
        int familiesCreated,
        int errorsCount,
        List<CsvRowError> errors,
        List<CsvRowPreview> preview
) {
    public record CsvRowError(int row, String field, String message) {}

    public record CsvRowPreview(
            int row,
            String email,
            String name,
            String role,
            String familyName,
            String familyRole,
            String sectionSlug,
            boolean valid,
            String error
    ) {}
}
