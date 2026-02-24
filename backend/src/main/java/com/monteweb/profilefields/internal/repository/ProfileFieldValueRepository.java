package com.monteweb.profilefields.internal.repository;

import com.monteweb.profilefields.internal.model.ProfileFieldValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProfileFieldValueRepository extends JpaRepository<ProfileFieldValue, UUID> {

    List<ProfileFieldValue> findByUserId(UUID userId);

    void deleteByFieldId(UUID fieldId);
}
