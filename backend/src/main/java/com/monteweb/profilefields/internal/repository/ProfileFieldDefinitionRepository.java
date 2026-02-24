package com.monteweb.profilefields.internal.repository;

import com.monteweb.profilefields.internal.model.ProfileFieldDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProfileFieldDefinitionRepository extends JpaRepository<ProfileFieldDefinition, UUID> {

    List<ProfileFieldDefinition> findByActiveTrueOrderByPositionAsc();

    List<ProfileFieldDefinition> findAllByOrderByPositionAsc();

    boolean existsByFieldKey(String fieldKey);
}
