package com.monteweb.forms.internal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "form_response_tracking")
@IdClass(FormResponseTrackingId.class)
@Getter
@Setter
@NoArgsConstructor
public class FormResponseTracking {

    @Id
    @Column(name = "form_id", nullable = false)
    private UUID formId;

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;
}
