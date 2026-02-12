package com.monteweb.fotobox.internal.repository;

import com.monteweb.fotobox.internal.model.FotoboxRoomSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FotoboxRoomSettingsRepository extends JpaRepository<FotoboxRoomSettings, UUID> {
    Optional<FotoboxRoomSettings> findByRoomId(UUID roomId);
}
