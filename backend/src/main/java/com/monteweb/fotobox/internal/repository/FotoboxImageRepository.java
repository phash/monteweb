package com.monteweb.fotobox.internal.repository;

import com.monteweb.fotobox.internal.model.FotoboxImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FotoboxImageRepository extends JpaRepository<FotoboxImage, UUID> {
    List<FotoboxImage> findByThreadIdOrderBySortOrderAscCreatedAtAsc(UUID threadId);
    int countByThreadId(UUID threadId);
    void deleteAllByThreadId(UUID threadId);

    List<FotoboxImage> findByUploadedBy(UUID uploadedBy);
}
