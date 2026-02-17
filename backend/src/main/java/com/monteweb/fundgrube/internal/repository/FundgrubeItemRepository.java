package com.monteweb.fundgrube.internal.repository;

import com.monteweb.fundgrube.internal.model.FundgrubeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FundgrubeItemRepository extends JpaRepository<FundgrubeItem, UUID> {

    @Query("SELECT i FROM FundgrubeItem i WHERE (i.expiresAt IS NULL OR i.expiresAt > :now) ORDER BY i.createdAt DESC")
    List<FundgrubeItem> findAllActive(@Param("now") Instant now);

    @Query("SELECT i FROM FundgrubeItem i WHERE i.sectionId = :sectionId AND (i.expiresAt IS NULL OR i.expiresAt > :now) ORDER BY i.createdAt DESC")
    List<FundgrubeItem> findActiveBySectionId(@Param("sectionId") UUID sectionId, @Param("now") Instant now);

    @Query("SELECT i FROM FundgrubeItem i WHERE i.expiresAt IS NOT NULL AND i.expiresAt < :now")
    List<FundgrubeItem> findExpired(@Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM FundgrubeItem i WHERE i.expiresAt IS NOT NULL AND i.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
