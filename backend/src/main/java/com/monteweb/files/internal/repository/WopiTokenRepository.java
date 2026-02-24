package com.monteweb.files.internal.repository;

import com.monteweb.files.internal.model.WopiToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

public interface WopiTokenRepository extends JpaRepository<WopiToken, String> {

    @Modifying
    @Query("DELETE FROM WopiToken t WHERE t.expiresAt < :now")
    int deleteExpired(Instant now);
}
