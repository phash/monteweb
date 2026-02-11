package com.monteweb.forms.internal.repository;

import com.monteweb.forms.internal.model.FormResponseTracking;
import com.monteweb.forms.internal.model.FormResponseTrackingId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FormResponseTrackingRepository extends JpaRepository<FormResponseTracking, FormResponseTrackingId> {

    boolean existsByFormIdAndUserId(UUID formId, UUID userId);
}
