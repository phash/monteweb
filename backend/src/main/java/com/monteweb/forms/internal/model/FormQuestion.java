package com.monteweb.forms.internal.model;

import com.monteweb.forms.QuestionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "form_questions")
@Getter
@Setter
@NoArgsConstructor
public class FormQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "form_id", nullable = false)
    private UUID formId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String label;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean required = false;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> options;
}
