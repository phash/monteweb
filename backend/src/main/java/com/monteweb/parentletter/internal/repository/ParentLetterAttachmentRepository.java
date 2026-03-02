package com.monteweb.parentletter.internal.repository;

import com.monteweb.parentletter.internal.model.ParentLetterAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ParentLetterAttachmentRepository extends JpaRepository<ParentLetterAttachment, UUID> {

    List<ParentLetterAttachment> findByLetterIdOrderBySortOrder(UUID letterId);

    long countByLetterId(UUID letterId);
}
