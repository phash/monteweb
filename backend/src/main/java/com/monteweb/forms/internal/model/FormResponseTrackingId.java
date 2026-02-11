package com.monteweb.forms.internal.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class FormResponseTrackingId implements Serializable {

    private UUID formId;
    private UUID userId;

    public FormResponseTrackingId() {
    }

    public FormResponseTrackingId(UUID formId, UUID userId) {
        this.formId = formId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormResponseTrackingId that = (FormResponseTrackingId) o;
        return Objects.equals(formId, that.formId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(formId, userId);
    }
}
