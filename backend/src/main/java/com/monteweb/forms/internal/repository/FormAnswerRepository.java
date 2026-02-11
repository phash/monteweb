package com.monteweb.forms.internal.repository;

import com.monteweb.forms.internal.model.FormAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FormAnswerRepository extends JpaRepository<FormAnswer, UUID> {

    List<FormAnswer> findByResponseId(UUID responseId);

    List<FormAnswer> findByQuestionId(UUID questionId);
}
