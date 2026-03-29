package com.monteweb.wiki.internal.repository;

import com.monteweb.wiki.internal.model.WikiPageVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WikiPageVersionRepository extends JpaRepository<WikiPageVersion, UUID> {

    List<WikiPageVersion> findByPageIdOrderByCreatedAtDesc(UUID pageId);

    List<WikiPageVersion> findByEditedBy(UUID userId);

    @Modifying
    @Query("UPDATE WikiPageVersion v SET v.editedBy = null WHERE v.editedBy = :userId")
    void anonymizeEditor(@Param("userId") UUID userId);
}
