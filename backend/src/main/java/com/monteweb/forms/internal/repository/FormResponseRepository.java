package com.monteweb.forms.internal.repository;

import com.monteweb.forms.internal.model.FormResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FormResponseRepository extends JpaRepository<FormResponse, UUID> {

    int countByFormId(UUID formId);

    Optional<FormResponse> findByFormIdAndUserId(UUID formId, UUID userId);

    boolean existsByFormIdAndUserId(UUID formId, UUID userId);

    List<FormResponse> findByFormIdOrderBySubmittedAtDesc(UUID formId);

    void deleteByFormId(UUID formId);
}
