package com.monteweb.wiki.internal.repository;

import com.monteweb.wiki.internal.model.WikiPageVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WikiPageVersionRepository extends JpaRepository<WikiPageVersion, UUID> {

    List<WikiPageVersion> findByPageIdOrderByCreatedAtDesc(UUID pageId);

    List<WikiPageVersion> findByEditedBy(UUID userId);
}
