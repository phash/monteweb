package com.monteweb.school;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API: Facade interface for the school module.
 */
public interface SchoolModuleApi {

    Optional<SchoolSectionInfo> findById(UUID id);

    Optional<SchoolSectionInfo> findBySlug(String slug);

    List<SchoolSectionInfo> findAllActive();
}
