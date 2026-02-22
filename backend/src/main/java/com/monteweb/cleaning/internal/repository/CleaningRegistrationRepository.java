package com.monteweb.cleaning.internal.repository;

import com.monteweb.cleaning.internal.model.CleaningRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CleaningRegistrationRepository extends JpaRepository<CleaningRegistration, UUID> {

    List<CleaningRegistration> findBySlotId(UUID slotId);

    List<CleaningRegistration> findByUserId(UUID userId);

    Optional<CleaningRegistration> findBySlotIdAndUserId(UUID slotId, UUID userId);

    boolean existsBySlotIdAndUserId(UUID slotId, UUID userId);

    long countBySlotId(UUID slotId);

    @Query("SELECT r FROM CleaningRegistration r JOIN CleaningSlot s ON r.slotId = s.id " +
            "WHERE r.userId = :userId AND s.slotDate >= :fromDate AND s.cancelled = false " +
            "ORDER BY s.slotDate ASC")
    List<CleaningRegistration> findUpcomingByUserId(@Param("userId") UUID userId,
                                                     @Param("fromDate") LocalDate fromDate);

    @Query("SELECT r FROM CleaningRegistration r WHERE r.slotId = :slotId AND r.swapOffered = true")
    List<CleaningRegistration> findSwapOffersForSlot(@Param("slotId") UUID slotId);

    @Query("SELECT COUNT(r) FROM CleaningRegistration r JOIN CleaningSlot s ON r.slotId = s.id " +
            "WHERE r.familyId = :familyId AND r.checkedOut = true " +
            "AND s.slotDate BETWEEN :from AND :to")
    long countCompletedByFamilyInRange(@Param("familyId") UUID familyId,
                                       @Param("from") LocalDate from,
                                       @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(r.actualMinutes), 0) FROM CleaningRegistration r " +
            "JOIN CleaningSlot s ON r.slotId = s.id " +
            "WHERE r.familyId = :familyId AND r.checkedOut = true " +
            "AND s.slotDate BETWEEN :from AND :to")
    int sumActualMinutesByFamilyInRange(@Param("familyId") UUID familyId,
                                        @Param("from") LocalDate from,
                                        @Param("to") LocalDate to);

    @Query("SELECT COUNT(r) FROM CleaningRegistration r JOIN CleaningSlot s ON r.slotId = s.id " +
            "WHERE s.sectionId = :sectionId AND r.noShow = true " +
            "AND s.slotDate BETWEEN :from AND :to")
    long countNoShowsBySectionInRange(@Param("sectionId") UUID sectionId,
                                      @Param("from") LocalDate from,
                                      @Param("to") LocalDate to);

    @Query("SELECT r FROM CleaningRegistration r " +
            "WHERE r.checkedOut = true AND r.confirmed = false AND r.noShow = false " +
            "ORDER BY r.checkOutAt ASC")
    List<CleaningRegistration> findPendingConfirmation();

    void deleteByUserId(UUID userId);
}
