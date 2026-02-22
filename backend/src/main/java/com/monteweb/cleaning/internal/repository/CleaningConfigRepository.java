package com.monteweb.cleaning.internal.repository;

import com.monteweb.cleaning.internal.model.CleaningConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CleaningConfigRepository extends JpaRepository<CleaningConfig, UUID> {

    List<CleaningConfig> findBySectionIdAndActiveTrue(UUID sectionId);

    List<CleaningConfig> findByActiveTrue();

    List<CleaningConfig> findBySectionId(UUID sectionId);

    List<CleaningConfig> findByRoomId(UUID roomId);
}
