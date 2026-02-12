package com.monteweb.fotobox.internal.repository;

import com.monteweb.fotobox.internal.model.FotoboxThread;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FotoboxThreadRepository extends JpaRepository<FotoboxThread, UUID> {
    List<FotoboxThread> findByRoomIdOrderByCreatedAtDesc(UUID roomId);
}
