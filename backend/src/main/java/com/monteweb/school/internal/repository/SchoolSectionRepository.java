package com.monteweb.school.internal.repository;

import com.monteweb.school.internal.model.SchoolSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolSectionRepository extends JpaRepository<SchoolSection, UUID> {

    Optional<SchoolSection> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<SchoolSection> findByActiveTrueOrderBySortOrderAsc();
}
