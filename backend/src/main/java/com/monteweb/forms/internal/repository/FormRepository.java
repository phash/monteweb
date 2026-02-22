package com.monteweb.forms.internal.repository;

import com.monteweb.forms.FormScope;
import com.monteweb.forms.FormStatus;
import com.monteweb.forms.internal.model.Form;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FormRepository extends JpaRepository<Form, UUID> {

    @Query(value = """
            SELECT * FROM forms f
            WHERE f.status IN ('PUBLISHED', 'CLOSED')
              AND (
                (f.scope = 'SCHOOL')
                OR (f.scope = 'SECTION' AND EXISTS (
                    SELECT 1 FROM unnest(f.section_ids) AS sid WHERE sid IN (:sectionIds)
                ))
                OR (f.scope = 'ROOM' AND f.scope_id IN (:roomIds))
              )
            ORDER BY f.published_at DESC
            """,
            countQuery = """
            SELECT count(*) FROM forms f
            WHERE f.status IN ('PUBLISHED', 'CLOSED')
              AND (
                (f.scope = 'SCHOOL')
                OR (f.scope = 'SECTION' AND EXISTS (
                    SELECT 1 FROM unnest(f.section_ids) AS sid WHERE sid IN (:sectionIds)
                ))
                OR (f.scope = 'ROOM' AND f.scope_id IN (:roomIds))
              )
            """,
            nativeQuery = true)
    Page<Form> findAvailableForms(
            @Param("roomIds") List<UUID> roomIds,
            @Param("sectionIds") List<UUID> sectionIds,
            Pageable pageable);

    Page<Form> findByCreatedByOrderByCreatedAtDesc(UUID createdBy, Pageable pageable);

    List<Form> findByScopeAndScopeIdAndStatus(FormScope scope, UUID scopeId, FormStatus status);

    @Query(value = """
            SELECT * FROM forms f
            WHERE f.scope = 'SECTION' AND f.status = 'PUBLISHED'
              AND :sectionId = ANY(f.section_ids)
            """, nativeQuery = true)
    List<Form> findPublishedForSection(@Param("sectionId") UUID sectionId);

    List<Form> findByScopeAndStatus(FormScope scope, FormStatus status);

    List<Form> findByStatusAndDeadlineBefore(FormStatus status, LocalDate deadline);
}
