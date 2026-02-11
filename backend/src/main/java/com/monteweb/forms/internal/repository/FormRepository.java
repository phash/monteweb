package com.monteweb.forms.internal.repository;

import com.monteweb.forms.FormScope;
import com.monteweb.forms.FormStatus;
import com.monteweb.forms.internal.model.Form;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FormRepository extends JpaRepository<Form, UUID> {

    @Query("""
            SELECT f FROM Form f
            WHERE f.status = 'PUBLISHED'
              AND (
                (f.scope = 'SCHOOL')
                OR (f.scope = 'SECTION' AND f.scopeId IN :sectionIds)
                OR (f.scope = 'ROOM' AND f.scopeId IN :roomIds)
              )
            ORDER BY f.publishedAt DESC
            """)
    Page<Form> findAvailableForms(
            @Param("roomIds") List<UUID> roomIds,
            @Param("sectionIds") List<UUID> sectionIds,
            Pageable pageable);

    Page<Form> findByCreatedByOrderByCreatedAtDesc(UUID createdBy, Pageable pageable);

    List<Form> findByScopeAndScopeIdAndStatus(FormScope scope, UUID scopeId, FormStatus status);

    List<Form> findByScopeAndStatus(FormScope scope, FormStatus status);
}
