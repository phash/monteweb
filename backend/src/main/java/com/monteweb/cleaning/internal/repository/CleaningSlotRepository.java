package com.monteweb.cleaning.internal.repository;

import com.monteweb.cleaning.internal.model.CleaningSlot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface CleaningSlotRepository extends JpaRepository<CleaningSlot, UUID> {

    @Query("SELECT s FROM CleaningSlot s WHERE s.sectionId = :sectionId AND s.slotDate >= :fromDate " +
            "AND s.cancelled = false ORDER BY s.slotDate ASC, s.startTime ASC")
    List<CleaningSlot> findUpcomingBySectionId(@Param("sectionId") UUID sectionId,
                                                @Param("fromDate") LocalDate fromDate,
                                                Pageable pageable);

    @Query("SELECT s FROM CleaningSlot s WHERE s.slotDate >= :fromDate AND s.cancelled = false " +
            "ORDER BY s.slotDate ASC, s.startTime ASC")
    Page<CleaningSlot> findUpcoming(@Param("fromDate") LocalDate fromDate, Pageable pageable);

    @Query("SELECT s FROM CleaningSlot s WHERE s.slotDate BETWEEN :from AND :to " +
            "AND s.cancelled = false ORDER BY s.slotDate ASC, s.startTime ASC")
    List<CleaningSlot> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT s FROM CleaningSlot s WHERE s.sectionId = :sectionId " +
            "AND s.slotDate BETWEEN :from AND :to AND s.cancelled = false " +
            "ORDER BY s.slotDate ASC, s.startTime ASC")
    List<CleaningSlot> findBySectionAndDateRange(@Param("sectionId") UUID sectionId,
                                                  @Param("from") LocalDate from,
                                                  @Param("to") LocalDate to);

    List<CleaningSlot> findByConfigIdAndSlotDateBetween(UUID configId, LocalDate from, LocalDate to);

    @Query("SELECT s FROM CleaningSlot s WHERE s.slotDate = :date AND s.status = 'OPEN' " +
            "AND s.cancelled = false AND " +
            "(SELECT COUNT(r) FROM CleaningRegistration r WHERE r.slotId = s.id) < s.minParticipants")
    List<CleaningSlot> findSlotsNeedingParticipants(@Param("date") LocalDate date);

    @Query("SELECT s FROM CleaningSlot s WHERE s.slotDate BETWEEN :from AND :to " +
            "AND s.cancelled = false AND s.status IN ('OPEN', 'FULL') " +
            "AND (SELECT COUNT(r) FROM CleaningRegistration r WHERE r.slotId = s.id) < s.minParticipants " +
            "ORDER BY s.slotDate ASC")
    List<CleaningSlot> findSlotsNeedingParticipantsInRange(@Param("from") LocalDate from,
                                                            @Param("to") LocalDate to);

    @Query("SELECT COUNT(s) FROM CleaningSlot s WHERE s.sectionId = :sectionId " +
            "AND s.slotDate BETWEEN :from AND :to AND s.status = 'COMPLETED'")
    long countCompletedSlots(@Param("sectionId") UUID sectionId,
                             @Param("from") LocalDate from,
                             @Param("to") LocalDate to);

    @Query("SELECT COUNT(s) FROM CleaningSlot s WHERE s.sectionId = :sectionId " +
            "AND s.slotDate BETWEEN :from AND :to AND s.cancelled = false")
    long countTotalSlots(@Param("sectionId") UUID sectionId,
                         @Param("from") LocalDate from,
                         @Param("to") LocalDate to);
}
