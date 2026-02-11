package com.monteweb.forms.internal.repository;

import com.monteweb.forms.internal.model.FormQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FormQuestionRepository extends JpaRepository<FormQuestion, UUID> {

    List<FormQuestion> findByFormIdOrderBySortOrder(UUID formId);

    void deleteByFormId(UUID formId);

    int countByFormId(UUID formId);
}
