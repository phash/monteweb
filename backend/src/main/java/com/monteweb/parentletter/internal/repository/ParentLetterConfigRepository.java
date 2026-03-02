package com.monteweb.parentletter.internal.repository;

import com.monteweb.parentletter.internal.model.ParentLetterConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ParentLetterConfigRepository extends JpaRepository<ParentLetterConfig, UUID> {

    /**
     * Find the section-specific config for a given section.
     */
    Optional<ParentLetterConfig> findBySectionId(UUID sectionId);

    /**
     * Find the global config (where sectionId is null).
     */
    Optional<ParentLetterConfig> findBySectionIdIsNull();
}
