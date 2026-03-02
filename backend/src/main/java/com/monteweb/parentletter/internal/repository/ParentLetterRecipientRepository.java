package com.monteweb.parentletter.internal.repository;

import com.monteweb.parentletter.RecipientStatus;
import com.monteweb.parentletter.internal.model.ParentLetterRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParentLetterRecipientRepository extends JpaRepository<ParentLetterRecipient, UUID> {

    List<ParentLetterRecipient> findByLetterIdOrderByCreatedAt(UUID letterId);

    List<ParentLetterRecipient> findByLetterIdAndStatus(UUID letterId, RecipientStatus status);

    Optional<ParentLetterRecipient> findByLetterIdAndParentIdAndStudentId(
            UUID letterId, UUID parentId, UUID studentId);

    List<ParentLetterRecipient> findByParentIdAndStatus(UUID parentId, RecipientStatus status);

    long countByLetterIdAndStatus(UUID letterId, RecipientStatus status);

    long countByLetterId(UUID letterId);

    /**
     * Count recipient rows for a given parent where status is NOT the given status.
     * Used for counting pending (non-confirmed) letters for a parent.
     */
    long countByParentIdAndStatusNot(UUID parentId, RecipientStatus status);

    @Modifying
    @Query("DELETE FROM ParentLetterRecipient r WHERE r.letter.id = :letterId")
    void deleteByLetterId(@Param("letterId") UUID letterId);

    @Modifying
    @Query("DELETE FROM ParentLetterRecipient r WHERE r.parentId = :parentId")
    void deleteByParentId(@Param("parentId") UUID parentId);

    @Modifying
    @Query("DELETE FROM ParentLetterRecipient r WHERE r.studentId = :studentId")
    void deleteByStudentId(@Param("studentId") UUID studentId);

    List<ParentLetterRecipient> findByParentId(UUID parentId);
}
