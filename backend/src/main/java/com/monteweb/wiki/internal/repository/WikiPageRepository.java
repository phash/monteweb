package com.monteweb.wiki.internal.repository;

import com.monteweb.wiki.internal.model.WikiPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WikiPageRepository extends JpaRepository<WikiPage, UUID> {

    List<WikiPage> findByRoomIdOrderByTitleAsc(UUID roomId);

    Optional<WikiPage> findByRoomIdAndSlug(UUID roomId, String slug);

    List<WikiPage> findByRoomIdAndParentIdIsNullOrderByTitleAsc(UUID roomId);

    List<WikiPage> findByParentIdOrderByTitleAsc(UUID parentId);

    boolean existsByRoomIdAndSlug(UUID roomId, String slug);

    List<WikiPage> findByCreatedBy(UUID userId);

    List<WikiPage> findByLastEditedBy(UUID userId);

    @Query("SELECT p FROM WikiPage p WHERE p.roomId = :roomId AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<WikiPage> searchByRoomIdAndQuery(UUID roomId, String query);
}
