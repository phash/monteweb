package com.monteweb.forms.internal.repository;

import com.monteweb.forms.internal.model.FormAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface FormAnswerRepository extends JpaRepository<FormAnswer, UUID> {

    List<FormAnswer> findByResponseId(UUID responseId);

    List<FormAnswer> findByQuestionId(UUID questionId);

    @Modifying
    @Query("DELETE FROM FormAnswer a WHERE a.responseId IN (SELECT r.id FROM FormResponse r WHERE r.formId = :formId)")
    void deleteByResponseFormId(UUID formId);

    void deleteByResponseId(UUID responseId);
}
