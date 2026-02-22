package com.monteweb.fundgrube.internal.repository;

import com.monteweb.fundgrube.internal.model.FundgrubeImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FundgrubeImageRepository extends JpaRepository<FundgrubeImage, UUID> {

    List<FundgrubeImage> findByItemIdOrderByCreatedAt(UUID itemId);

    void deleteByItemId(UUID itemId);

    List<FundgrubeImage> findByItemIdIn(List<UUID> itemIds);
}
