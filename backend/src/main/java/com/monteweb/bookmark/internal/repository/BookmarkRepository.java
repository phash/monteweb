package com.monteweb.bookmark.internal.repository;

import com.monteweb.bookmark.internal.model.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookmarkRepository extends JpaRepository<Bookmark, UUID> {

    Optional<Bookmark> findByUserIdAndContentTypeAndContentId(UUID userId, String contentType, UUID contentId);

    Page<Bookmark> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Bookmark> findByUserIdAndContentTypeOrderByCreatedAtDesc(UUID userId, String contentType, Pageable pageable);

    boolean existsByUserIdAndContentTypeAndContentId(UUID userId, String contentType, UUID contentId);

    @Query("SELECT b.contentId FROM Bookmark b WHERE b.userId = :userId AND b.contentType = :contentType")
    List<UUID> findContentIdsByUserIdAndContentType(UUID userId, String contentType);

    List<Bookmark> findByUserId(UUID userId);

    void deleteByContentTypeAndContentId(String contentType, UUID contentId);
}
