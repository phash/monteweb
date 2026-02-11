package com.monteweb.forms.internal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "form_answers")
@Getter
@Setter
@NoArgsConstructor
public class FormAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "response_id", nullable = false)
    private UUID responseId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answer_options", columnDefinition = "jsonb")
    private List<String> answerOptions;

    @Column(name = "answer_rating")
    private Integer answerRating;
}
