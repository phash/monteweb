package com.monteweb.calendar.internal.repository;

import com.monteweb.calendar.EventScope;
import com.monteweb.calendar.internal.model.CalendarEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {

    @Query("""
            SELECT e FROM CalendarEvent e
            WHERE e.cancelled = false
              AND e.startDate <= :to AND e.endDate >= :from
              AND (
                (e.scope = 'SCHOOL')
                OR (e.scope = 'SECTION' AND e.scopeId IN :sectionIds)
                OR (e.scope = 'ROOM' AND e.scopeId IN :roomIds)
              )
            ORDER BY e.startDate ASC, e.startTime ASC NULLS FIRST
            """)
    Page<CalendarEvent> findPersonalEvents(
            @Param("roomIds") List<UUID> roomIds,
            @Param("sectionIds") List<UUID> sectionIds,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    @Query("""
            SELECT e FROM CalendarEvent e
            WHERE e.scope = :scope AND e.scopeId = :scopeId
              AND e.cancelled = false
              AND e.startDate <= :to AND e.endDate >= :from
            ORDER BY e.startDate ASC, e.startTime ASC NULLS FIRST
            """)
    List<CalendarEvent> findByScopeAndScopeIdAndDateRange(
            @Param("scope") EventScope scope,
            @Param("scopeId") UUID scopeId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            SELECT e FROM CalendarEvent e
            WHERE e.scope = 'SCHOOL'
              AND e.cancelled = false
              AND e.startDate <= :to AND e.endDate >= :from
            ORDER BY e.startDate ASC, e.startTime ASC NULLS FIRST
            """)
    List<CalendarEvent> findSchoolEvents(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            SELECT e FROM CalendarEvent e
            WHERE e.scope = 'ROOM' AND e.scopeId = :roomId
              AND e.startDate <= :to AND e.endDate >= :from
            ORDER BY e.startDate ASC, e.startTime ASC NULLS FIRST
            """)
    Page<CalendarEvent> findByRoomId(
            @Param("roomId") UUID roomId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);
}
