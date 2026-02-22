package com.monteweb.calendar.internal.service;

import com.monteweb.calendar.*;
import com.monteweb.calendar.internal.model.CalendarEvent;
import com.monteweb.calendar.internal.model.EventRsvp;
import com.monteweb.calendar.internal.repository.CalendarEventRepository;
import com.monteweb.calendar.internal.repository.EventRsvpRepository;
import com.monteweb.jobboard.JobboardModuleApi;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.room.RoomRole;
import com.monteweb.school.SchoolModuleApi;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@ConditionalOnProperty(prefix = "monteweb.modules", name = "calendar.enabled", havingValue = "true")
public class CalendarService implements CalendarModuleApi {

    private final CalendarEventRepository eventRepository;
    private final EventRsvpRepository rsvpRepository;
    private final RoomModuleApi roomModule;
    private final SchoolModuleApi schoolModule;
    private final UserModuleApi userModule;
    private final ApplicationEventPublisher eventPublisher;
    private final JobboardModuleApi jobboardModuleApi;

    public CalendarService(CalendarEventRepository eventRepository,
                           EventRsvpRepository rsvpRepository,
                           RoomModuleApi roomModule,
                           SchoolModuleApi schoolModule,
                           UserModuleApi userModule,
                           ApplicationEventPublisher eventPublisher,
                           @Lazy @Autowired(required = false) JobboardModuleApi jobboardModuleApi) {
        this.eventRepository = eventRepository;
        this.rsvpRepository = rsvpRepository;
        this.roomModule = roomModule;
        this.schoolModule = schoolModule;
        this.userModule = userModule;
        this.eventPublisher = eventPublisher;
        this.jobboardModuleApi = jobboardModuleApi;
    }

    public Page<EventInfo> getPersonalEvents(UUID userId, LocalDate from, LocalDate to, Pageable pageable) {
        var rooms = roomModule.findByUserId(userId);
        var roomIds = rooms.stream().map(r -> r.id()).toList();
        var sectionIds = rooms.stream()
                .filter(r -> r.sectionId() != null)
                .map(r -> r.sectionId())
                .distinct()
                .toList();

        // Ensure non-empty lists for IN clause
        if (roomIds.isEmpty()) roomIds = List.of(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        if (sectionIds.isEmpty()) sectionIds = List.of(UUID.fromString("00000000-0000-0000-0000-000000000000"));

        return eventRepository.findPersonalEvents(roomIds, sectionIds, from, to, pageable)
                .map(e -> toEventInfo(e, userId));
    }

    public Page<EventInfo> getRoomEvents(UUID roomId, UUID userId, LocalDate from, LocalDate to, Pageable pageable) {
        if (!roomModule.isUserInRoom(userId, roomId)) {
            throw new IllegalArgumentException("User is not a member of this room");
        }
        return eventRepository.findByRoomId(roomId, from, to, pageable)
                .map(e -> toEventInfo(e, userId));
    }

    public EventInfo getEvent(UUID eventId, UUID userId) {
        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        return toEventInfo(event, userId);
    }

    public EventInfo createEvent(CreateEventRequest request, UUID userId) {
        checkCreatePermission(request.scope(), request.scopeId(), userId);

        var event = new CalendarEvent();
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setLocation(request.location());
        event.setAllDay(request.allDay());
        event.setStartDate(request.startDate());
        event.setStartTime(request.allDay() ? null : request.startTime());
        event.setEndDate(request.endDate());
        event.setEndTime(request.allDay() ? null : request.endTime());
        event.setScope(request.scope());
        event.setScopeId(request.scopeId());
        event.setRecurrence(request.recurrence() != null ? request.recurrence() : EventRecurrence.NONE);
        event.setRecurrenceEnd(request.recurrenceEnd());
        event.setCreatedBy(userId);

        event = eventRepository.save(event);

        var user = userModule.findById(userId).orElse(null);
        String creatorName = user != null ? user.displayName() : "Unknown";

        eventPublisher.publishEvent(new EventCreatedEvent(
                event.getId(), event.getTitle(), event.getScope(), event.getScopeId(),
                userId, creatorName
        ));

        return toEventInfo(event, userId);
    }

    public EventInfo updateEvent(UUID eventId, UpdateEventRequest request, UUID userId) {
        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        checkCreatePermission(event.getScope(), event.getScopeId(), userId);

        if (request.title() != null) event.setTitle(request.title());
        if (request.description() != null) event.setDescription(request.description());
        if (request.location() != null) event.setLocation(request.location());
        if (request.allDay() != null) event.setAllDay(request.allDay());
        if (request.startDate() != null) event.setStartDate(request.startDate());
        if (request.startTime() != null) event.setStartTime(request.startTime());
        if (request.endDate() != null) event.setEndDate(request.endDate());
        if (request.endTime() != null) event.setEndTime(request.endTime());
        if (request.recurrence() != null) event.setRecurrence(request.recurrence());
        if (request.recurrenceEnd() != null) event.setRecurrenceEnd(request.recurrenceEnd());

        event = eventRepository.save(event);
        return toEventInfo(event, userId);
    }

    public EventInfo cancelEvent(UUID eventId, UUID userId) {
        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        checkCreatePermission(event.getScope(), event.getScopeId(), userId);

        event.setCancelled(true);
        event = eventRepository.save(event);

        var user = userModule.findById(userId).orElse(null);
        String cancellerName = user != null ? user.displayName() : "Unknown";

        eventPublisher.publishEvent(new EventCancelledEvent(
                event.getId(), event.getTitle(), event.getScope(), event.getScopeId(),
                userId, cancellerName
        ));

        return toEventInfo(event, userId);
    }

    public void deleteEvent(UUID eventId, UUID userId) {
        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        checkCreatePermission(event.getScope(), event.getScopeId(), userId);

        // Capture attending users before deletion for targeted feed post
        List<UUID> attendingUserIds = rsvpRepository.findUserIdsByEventIdAndStatus(eventId, RsvpStatus.ATTENDING);

        var user = userModule.findById(userId).orElse(null);
        String deleterName = user != null ? user.displayName() : "Unknown";

        eventRepository.delete(event);

        eventPublisher.publishEvent(new EventDeletedEvent(
                eventId, event.getTitle(), event.getScope(), event.getScopeId(),
                userId, deleterName, attendingUserIds
        ));
    }

    public EventInfo rsvp(UUID eventId, UUID userId, RsvpStatus status) {
        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        if (event.isCancelled()) {
            throw new IllegalStateException("Cannot RSVP to a cancelled event");
        }

        var rsvp = rsvpRepository.findByEventIdAndUserId(eventId, userId)
                .orElseGet(() -> {
                    var r = new EventRsvp();
                    r.setEventId(eventId);
                    r.setUserId(userId);
                    return r;
                });

        rsvp.setStatus(status);
        rsvpRepository.save(rsvp);

        return toEventInfo(event, userId);
    }

    // CalendarModuleApi implementations

    @Override
    @Transactional(readOnly = true)
    public List<EventInfo> getUpcomingEventsForRoom(UUID roomId, LocalDate from, LocalDate to) {
        return eventRepository.findByScopeAndScopeIdAndDateRange(EventScope.ROOM, roomId, from, to)
                .stream().map(e -> toEventInfo(e, null)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventInfo> getUpcomingEventsForSection(UUID sectionId, LocalDate from, LocalDate to) {
        return eventRepository.findByScopeAndScopeIdAndDateRange(EventScope.SECTION, sectionId, from, to)
                .stream().map(e -> toEventInfo(e, null)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventInfo> getUpcomingEventsForSchool(LocalDate from, LocalDate to) {
        return eventRepository.findSchoolEvents(from, to)
                .stream().map(e -> toEventInfo(e, null)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EventInfo> findById(UUID eventId) {
        return eventRepository.findById(eventId)
                .map(e -> toEventInfo(e, null));
    }

    @Override
    public EventInfo createEventFromSystem(CreateEventRequest request, UUID createdBy) {
        var event = new CalendarEvent();
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setLocation(request.location());
        event.setAllDay(request.allDay());
        event.setStartDate(request.startDate());
        event.setStartTime(request.allDay() ? null : request.startTime());
        event.setEndDate(request.endDate());
        event.setEndTime(request.allDay() ? null : request.endTime());
        event.setScope(request.scope());
        event.setScopeId(request.scopeId());
        event.setRecurrence(request.recurrence() != null ? request.recurrence() : EventRecurrence.NONE);
        event.setRecurrenceEnd(request.recurrenceEnd());
        if (request.eventType() != null) {
            event.setEventType(request.eventType());
        }
        event.setCreatedBy(createdBy);

        event = eventRepository.save(event);

        var user = userModule.findById(createdBy).orElse(null);
        String creatorName = user != null ? user.displayName() : "System";

        eventPublisher.publishEvent(new EventCreatedEvent(
                event.getId(), event.getTitle(), event.getScope(), event.getScopeId(),
                createdBy, creatorName
        ));

        return toEventInfo(event, createdBy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventInfo> getEventsForUserIds(List<UUID> userIds, LocalDate from, LocalDate to) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        var eventIds = rsvpRepository.findEventIdsByUserIdsAndAccepted(userIds);
        if (eventIds.isEmpty()) {
            return List.of();
        }
        return eventRepository.findByIdsAndDateRange(eventIds, from, to)
                .stream().map(e -> toEventInfo(e, null)).toList();
    }

    private void checkCreatePermission(EventScope scope, UUID scopeId, UUID userId) {
        var user = userModule.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.role() == UserRole.SUPERADMIN) return;

        // ELTERNBEIRAT special role can create/edit events
        if (hasSpecialRole(user, "ELTERNBEIRAT", scopeId)) {
            return;
        }

        switch (scope) {
            case ROOM -> {
                if (scopeId == null) throw new IllegalArgumentException("Room ID required for ROOM scope");
                var role = roomModule.getUserRoleInRoom(userId, scopeId)
                        .orElseThrow(() -> new IllegalArgumentException("User is not a member of this room"));
                if (role != RoomRole.LEADER) {
                    throw new IllegalArgumentException("Only room leaders can create room events");
                }
            }
            case SECTION -> {
                if (user.role() != UserRole.TEACHER && user.role() != UserRole.SECTION_ADMIN) {
                    throw new IllegalArgumentException("Only teachers or Elternbeirat can create section events");
                }
            }
            case SCHOOL -> {
                throw new IllegalArgumentException("Only admins can create school-wide events");
            }
        }
    }

    private boolean hasSpecialRole(com.monteweb.user.UserInfo user, String roleName, UUID scopeId) {
        if (user.specialRoles() == null) return false;
        if (user.specialRoles().contains(roleName)) return true;
        return scopeId != null && user.specialRoles().contains(roleName + ":" + scopeId);
    }

    private EventInfo toEventInfo(CalendarEvent event, UUID currentUserId) {
        var creator = userModule.findById(event.getCreatedBy()).orElse(null);
        String creatorName = creator != null ? creator.displayName() : "Unknown";

        String scopeName = resolveScopeName(event.getScope(), event.getScopeId());

        int attendingCount = (int) rsvpRepository.countByEventIdAndStatus(event.getId(), RsvpStatus.ATTENDING);
        int maybeCount = (int) rsvpRepository.countByEventIdAndStatus(event.getId(), RsvpStatus.MAYBE);
        int declinedCount = (int) rsvpRepository.countByEventIdAndStatus(event.getId(), RsvpStatus.DECLINED);

        RsvpStatus currentUserRsvp = null;
        if (currentUserId != null) {
            currentUserRsvp = rsvpRepository.findByEventIdAndUserId(event.getId(), currentUserId)
                    .map(EventRsvp::getStatus)
                    .orElse(null);
        }

        int linkedJobCount = 0;
        if (jobboardModuleApi != null) {
            linkedJobCount = jobboardModuleApi.countJobsForEvent(event.getId());
        }

        return new EventInfo(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.isAllDay(),
                event.getStartDate(),
                event.getStartTime(),
                event.getEndDate(),
                event.getEndTime(),
                event.getScope(),
                event.getScopeId(),
                scopeName,
                event.getRecurrence(),
                event.getRecurrenceEnd(),
                event.isCancelled(),
                event.getEventType(),
                event.getCreatedBy(),
                creatorName,
                attendingCount,
                maybeCount,
                declinedCount,
                currentUserRsvp,
                linkedJobCount,
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    private String resolveScopeName(EventScope scope, UUID scopeId) {
        if (scopeId == null) return null;
        return switch (scope) {
            case ROOM -> roomModule.findById(scopeId).map(r -> r.name()).orElse(null);
            case SECTION -> schoolModule.findById(scopeId).map(s -> s.name()).orElse(null);
            case SCHOOL -> null;
        };
    }

    /**
     * DSGVO: Clean up all calendar data for a deleted user.
     */
    @Transactional
    public void cleanupUserData(UUID userId) {
        // Anonymize events created by user
        var events = eventRepository.findByCreatedBy(userId);
        for (var event : events) {
            event.setCreatedBy(null);
        }
        eventRepository.saveAll(events);
        // Delete RSVPs
        rsvpRepository.deleteByUserId(userId);
    }

    /**
     * DSGVO: Export all calendar data for a user.
     */
    @Override
    public Map<String, Object> exportUserData(UUID userId) {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        var events = eventRepository.findByCreatedBy(userId);
        data.put("eventsCreated", events.stream().map(e -> Map.of(
                "id", e.getId(),
                "title", e.getTitle(),
                "createdAt", e.getCreatedAt()
        )).toList());
        var rsvps = rsvpRepository.findByUserId(userId);
        data.put("rsvps", rsvps.stream().map(r -> Map.of(
                "eventId", r.getEventId(),
                "status", r.getStatus().name()
        )).toList());
        return data;
    }
}
