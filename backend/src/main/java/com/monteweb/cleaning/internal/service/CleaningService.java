package com.monteweb.cleaning.internal.service;

import com.monteweb.cleaning.*;
import com.monteweb.cleaning.internal.model.CleaningConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.monteweb.cleaning.internal.model.CleaningRegistration;
import com.monteweb.cleaning.internal.model.CleaningSlot;
import com.monteweb.cleaning.internal.repository.CleaningConfigRepository;
import com.monteweb.cleaning.internal.repository.CleaningRegistrationRepository;
import com.monteweb.cleaning.internal.repository.CleaningSlotRepository;
import com.monteweb.calendar.CalendarModuleApi;
import com.monteweb.calendar.CreateEventRequest;
import com.monteweb.calendar.EventScope;
import com.monteweb.school.SchoolModuleApi;
import com.monteweb.school.SchoolSectionInfo;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import com.monteweb.shared.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(prefix = "monteweb.modules.cleaning", name = "enabled", havingValue = "true")
@Transactional
public class CleaningService implements CleaningModuleApi {

    private final CleaningConfigRepository configRepository;
    private final CleaningSlotRepository slotRepository;
    private final CleaningRegistrationRepository registrationRepository;
    private final QrTokenService qrTokenService;
    private final SchoolModuleApi schoolModuleApi;
    private final ApplicationEventPublisher eventPublisher;
    private final CalendarModuleApi calendarModuleApi;

    public CleaningService(CleaningConfigRepository configRepository,
                           CleaningSlotRepository slotRepository,
                           CleaningRegistrationRepository registrationRepository,
                           QrTokenService qrTokenService,
                           SchoolModuleApi schoolModuleApi,
                           ApplicationEventPublisher eventPublisher,
                           @Autowired(required = false) CalendarModuleApi calendarModuleApi) {
        this.configRepository = configRepository;
        this.slotRepository = slotRepository;
        this.registrationRepository = registrationRepository;
        this.qrTokenService = qrTokenService;
        this.schoolModuleApi = schoolModuleApi;
        this.eventPublisher = eventPublisher;
        this.calendarModuleApi = calendarModuleApi;
    }

    // ── Config Management ───────────────────────────────────────────────

    public CleaningConfigInfo createConfig(CreateConfigRequest request) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId();
        CleaningConfig config = new CleaningConfig();
        config.setSectionId(request.sectionId());
        config.setTitle(request.title());
        config.setDescription(request.description());
        config.setStartTime(request.startTime());
        config.setEndTime(request.endTime());
        config.setMinParticipants(request.minParticipants());
        config.setMaxParticipants(request.maxParticipants());
        config.setHoursCredit(request.hoursCredit());
        if (request.specificDate() != null) {
            config.setSpecificDate(request.specificDate());
            config.setDayOfWeek(request.specificDate().getDayOfWeek().getValue());
        } else {
            config.setDayOfWeek(request.dayOfWeek());
        }
        config = configRepository.save(config);

        // For Putzaktionen (one-time with specificDate): create calendar event + publish event for jobboard
        if (request.specificDate() != null && calendarModuleApi != null) {
            var calendarRequest = new CreateEventRequest(
                    request.title(),
                    request.description(),
                    null, // location
                    false, // allDay
                    request.specificDate(),
                    request.startTime(),
                    request.specificDate(),
                    request.endTime(),
                    EventScope.SECTION,
                    request.sectionId(),
                    null, // recurrence
                    null  // recurrenceEnd
            );
            var calendarEvent = calendarModuleApi.createEventFromSystem(calendarRequest, currentUserId);
            config.setCalendarEventId(calendarEvent.id());
            config = configRepository.save(config);

            eventPublisher.publishEvent(new PutzaktionCreatedEvent(
                    config.getId(),
                    request.sectionId(),
                    request.title(),
                    request.description(),
                    request.specificDate(),
                    request.startTime(),
                    request.endTime(),
                    request.maxParticipants(),
                    request.hoursCredit(),
                    calendarEvent.id(),
                    currentUserId
            ));
        }

        return toConfigInfo(config);
    }

    public CleaningConfigInfo updateConfig(UUID configId, UpdateConfigRequest request) {
        CleaningConfig config = configRepository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("CleaningConfig", configId));
        if (request.title() != null) config.setTitle(request.title());
        if (request.description() != null) config.setDescription(request.description());
        if (request.startTime() != null) config.setStartTime(request.startTime());
        if (request.endTime() != null) config.setEndTime(request.endTime());
        if (request.minParticipants() != null) config.setMinParticipants(request.minParticipants());
        if (request.maxParticipants() != null) config.setMaxParticipants(request.maxParticipants());
        if (request.hoursCredit() != null) config.setHoursCredit(request.hoursCredit());
        if (request.active() != null) config.setActive(request.active());
        config = configRepository.save(config);
        return toConfigInfo(config);
    }

    @Transactional(readOnly = true)
    public List<CleaningConfigInfo> getConfigsBySection(UUID sectionId) {
        return configRepository.findBySectionId(sectionId).stream()
                .map(this::toConfigInfo)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CleaningConfigInfo> getAllActiveConfigs() {
        return configRepository.findByActiveTrue().stream()
                .map(this::toConfigInfo)
                .toList();
    }

    // ── Slot Generation ─────────────────────────────────────────────────

    /**
     * Generates cleaning slots for a config in the given date range.
     * Skips dates where a slot already exists for this config.
     */
    public List<CleaningSlotInfo> generateSlots(UUID configId, LocalDate from, LocalDate to) {
        CleaningConfig config = configRepository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("CleaningConfig", configId));

        if (!config.isActive()) {
            throw new BusinessException("Cannot generate slots for inactive config");
        }

        // Find existing slots to avoid duplicates
        List<CleaningSlot> existing = slotRepository.findByConfigIdAndSlotDateBetween(configId, from, to);
        var existingDates = existing.stream()
                .map(CleaningSlot::getSlotDate)
                .collect(Collectors.toSet());

        List<CleaningSlot> generated = new ArrayList<>();

        if (config.getSpecificDate() != null) {
            // One-time cleaning action: generate exactly one slot for the specific date
            LocalDate target = config.getSpecificDate();
            if (!target.isBefore(from) && !target.isAfter(to) && !existingDates.contains(target)) {
                CleaningSlot slot = new CleaningSlot();
                slot.setConfigId(configId);
                slot.setSectionId(config.getSectionId());
                slot.setSlotDate(target);
                slot.setStartTime(config.getStartTime());
                slot.setEndTime(config.getEndTime());
                slot.setMinParticipants(config.getMinParticipants());
                slot.setMaxParticipants(config.getMaxParticipants());
                slot.setQrToken(qrTokenService.generateToken(UUID.randomUUID()));
                generated.add(slot);
            }
        } else {
            // Recurring: iterate by day-of-week
            DayOfWeek targetDay = DayOfWeek.of(config.getDayOfWeek());
            LocalDate current = from;
            while (!current.isAfter(to)) {
                if (current.getDayOfWeek() == targetDay && !existingDates.contains(current)) {
                    CleaningSlot slot = new CleaningSlot();
                    slot.setConfigId(configId);
                    slot.setSectionId(config.getSectionId());
                    slot.setSlotDate(current);
                    slot.setStartTime(config.getStartTime());
                    slot.setEndTime(config.getEndTime());
                    slot.setMinParticipants(config.getMinParticipants());
                    slot.setMaxParticipants(config.getMaxParticipants());
                    slot.setQrToken(qrTokenService.generateToken(slot.getId() != null ? slot.getId() : UUID.randomUUID()));
                    generated.add(slot);
                }
                current = current.plusDays(1);
            }
        }

        List<CleaningSlot> saved = slotRepository.saveAll(generated);
        // Regenerate QR tokens with actual IDs
        for (CleaningSlot slot : saved) {
            slot.setQrToken(qrTokenService.generateToken(slot.getId()));
        }
        slotRepository.saveAll(saved);

        return saved.stream().map(s -> toSlotInfo(s, config.getTitle())).toList();
    }

    // ── Slot Queries ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<CleaningSlotInfo> getUpcomingSlotsForSection(UUID sectionId, int limit) {
        List<CleaningSlot> slots = slotRepository.findUpcomingBySectionId(
                sectionId, LocalDate.now(), PageRequest.of(0, limit));
        return slots.stream().map(s -> toSlotInfo(s, getConfigTitle(s.getConfigId()))).toList();
    }

    @Transactional(readOnly = true)
    public Page<CleaningSlotInfo> getUpcomingSlots(Pageable pageable) {
        return slotRepository.findUpcoming(LocalDate.now(), pageable)
                .map(s -> toSlotInfo(s, getConfigTitle(s.getConfigId())));
    }

    @Transactional(readOnly = true)
    public CleaningSlotInfo getSlotById(UUID slotId) {
        CleaningSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("CleaningSlot", slotId));
        return toSlotInfo(slot, getConfigTitle(slot.getConfigId()));
    }

    @Transactional(readOnly = true)
    public List<CleaningSlotInfo> getMySlots(UUID userId) {
        List<CleaningRegistration> registrations = registrationRepository.findUpcomingByUserId(userId, LocalDate.now());
        return registrations.stream()
                .map(r -> {
                    CleaningSlot slot = slotRepository.findById(r.getSlotId()).orElse(null);
                    return slot != null ? toSlotInfo(slot, getConfigTitle(slot.getConfigId())) : null;
                })
                .filter(s -> s != null)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CleaningSlotInfo> getSlotsNeedingParticipants(LocalDate from, LocalDate to) {
        return slotRepository.findSlotsNeedingParticipantsInRange(from, to).stream()
                .map(s -> toSlotInfo(s, getConfigTitle(s.getConfigId())))
                .toList();
    }

    // ── Registration ────────────────────────────────────────────────────

    public CleaningSlotInfo registerForSlot(UUID slotId, UUID userId, String userName, UUID familyId) {
        CleaningSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("CleaningSlot", slotId));

        if (slot.isCancelled()) {
            throw new BusinessException("Cannot register for a cancelled slot");
        }
        if ("COMPLETED".equals(slot.getStatus()) || "IN_PROGRESS".equals(slot.getStatus())) {
            throw new BusinessException("Cannot register for a slot that is already in progress or completed");
        }
        if (registrationRepository.existsBySlotIdAndUserId(slotId, userId)) {
            throw new BusinessException("Already registered for this slot");
        }
        long currentCount = registrationRepository.countBySlotId(slotId);
        if (currentCount >= slot.getMaxParticipants()) {
            throw new BusinessException("Slot is full");
        }

        CleaningRegistration reg = new CleaningRegistration();
        reg.setSlotId(slotId);
        reg.setUserId(userId);
        reg.setUserName(userName);
        reg.setFamilyId(familyId);
        registrationRepository.save(reg);

        // Update slot status if full
        if (currentCount + 1 >= slot.getMaxParticipants()) {
            slot.setStatus("FULL");
            slotRepository.save(slot);
        }

        return toSlotInfo(slot, getConfigTitle(slot.getConfigId()));
    }

    public void unregisterFromSlot(UUID slotId, UUID userId) {
        CleaningRegistration reg = registrationRepository.findBySlotIdAndUserId(slotId, userId)
                .orElseThrow(() -> new BusinessException("Not registered for this slot"));

        if (reg.isCheckedIn()) {
            throw new BusinessException("Cannot unregister after check-in");
        }

        registrationRepository.delete(reg);

        // Update slot status back to OPEN if it was FULL
        CleaningSlot slot = slotRepository.findById(slotId).orElse(null);
        if (slot != null && "FULL".equals(slot.getStatus())) {
            slot.setStatus("OPEN");
            slotRepository.save(slot);
        }
    }

    public void offerSwap(UUID slotId, UUID userId) {
        CleaningRegistration reg = registrationRepository.findBySlotIdAndUserId(slotId, userId)
                .orElseThrow(() -> new BusinessException("Not registered for this slot"));

        if (reg.isCheckedIn()) {
            throw new BusinessException("Cannot offer swap after check-in");
        }
        reg.setSwapOffered(true);
        registrationRepository.save(reg);
    }

    @Transactional(readOnly = true)
    public List<CleaningSlotInfo.RegistrationInfo> getSwapOffers(UUID slotId) {
        return registrationRepository.findSwapOffersForSlot(slotId).stream()
                .map(this::toRegistrationInfo)
                .toList();
    }

    // ── Check-in / Check-out ────────────────────────────────────────────

    /**
     * QR-based check-in. Validates the QR token against the slot.
     */
    public CleaningSlotInfo checkIn(UUID slotId, UUID userId, String qrToken) {
        CleaningSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("CleaningSlot", slotId));

        // Validate QR token
        UUID tokenSlotId = qrTokenService.validateToken(qrToken);
        if (tokenSlotId == null || !tokenSlotId.equals(slotId)) {
            // Also accept if the slot's stored token matches
            if (slot.getQrToken() == null || !slot.getQrToken().equals(qrToken)) {
                throw new BusinessException("Invalid QR token");
            }
        }

        CleaningRegistration reg = registrationRepository.findBySlotIdAndUserId(slotId, userId)
                .orElseThrow(() -> new BusinessException("Not registered for this slot"));

        if (reg.isCheckedIn()) {
            throw new BusinessException("Already checked in");
        }

        reg.setCheckedIn(true);
        reg.setCheckInAt(java.time.Instant.now());
        registrationRepository.save(reg);

        // Update slot status to IN_PROGRESS if first check-in
        if ("OPEN".equals(slot.getStatus()) || "FULL".equals(slot.getStatus())) {
            slot.setStatus("IN_PROGRESS");
            slotRepository.save(slot);
        }

        return toSlotInfo(slot, getConfigTitle(slot.getConfigId()));
    }

    /**
     * Check-out with actual duration recording.
     */
    public CleaningSlotInfo checkOut(UUID slotId, UUID userId) {
        CleaningSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("CleaningSlot", slotId));

        CleaningRegistration reg = registrationRepository.findBySlotIdAndUserId(slotId, userId)
                .orElseThrow(() -> new BusinessException("Not registered for this slot"));

        if (!reg.isCheckedIn()) {
            throw new BusinessException("Must check in before checking out");
        }
        if (reg.isCheckedOut()) {
            throw new BusinessException("Already checked out");
        }

        reg.setCheckedOut(true);
        reg.setCheckOutAt(java.time.Instant.now());

        // Calculate actual minutes from check-in to check-out
        int actualMinutes = (int) Duration.between(reg.getCheckInAt(), reg.getCheckOutAt()).toMinutes();
        reg.setActualMinutes(actualMinutes);
        registrationRepository.save(reg);

        // Get hours credit from config
        CleaningConfig config = configRepository.findById(slot.getConfigId()).orElse(null);
        BigDecimal hoursCredit = config != null ? config.getHoursCredit() : BigDecimal.ZERO;

        // Check if all checked-in users have checked out -> complete slot
        List<CleaningRegistration> allRegs = registrationRepository.findBySlotId(slotId);
        boolean allCheckedOut = allRegs.stream()
                .filter(CleaningRegistration::isCheckedIn)
                .allMatch(CleaningRegistration::isCheckedOut);

        if (allCheckedOut) {
            slot.setStatus("COMPLETED");
            slotRepository.save(slot);

            // Mark no-shows
            for (CleaningRegistration r : allRegs) {
                if (!r.isCheckedIn()) {
                    r.setNoShow(true);
                    registrationRepository.save(r);
                }
            }
        }

        // Publish event for hour credit
        eventPublisher.publishEvent(new CleaningCompletedEvent(
                slotId, userId, reg.getUserName(), reg.getFamilyId(),
                hoursCredit, actualMinutes));

        return toSlotInfo(slot, config != null ? config.getTitle() : "");
    }

    /**
     * Get all cleaning registrations that are checked out but not yet confirmed.
     */
    public List<CleaningSlotInfo.RegistrationInfo> getPendingCleaningConfirmations() {
        return registrationRepository.findPendingConfirmation().stream()
                .map(this::toRegistrationInfo)
                .toList();
    }

    /**
     * Confirm a cleaning registration (admin/teacher/putzorga).
     */
    public CleaningSlotInfo.RegistrationInfo confirmCleaningRegistration(UUID registrationId, UUID confirmerId) {
        CleaningRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("CleaningRegistration", registrationId));

        if (!reg.isCheckedOut()) {
            throw new BusinessException("Only checked-out registrations can be confirmed");
        }
        if (reg.isConfirmed()) {
            throw new BusinessException("Registration is already confirmed");
        }

        reg.setConfirmed(true);
        reg.setConfirmedBy(confirmerId);
        reg.setConfirmedAt(java.time.Instant.now());
        registrationRepository.save(reg);
        return toRegistrationInfo(reg);
    }

    /**
     * Reject a cleaning registration – sets noShow=true and resets checkout data.
     */
    public void rejectCleaningRegistration(UUID registrationId) {
        CleaningRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("CleaningRegistration", registrationId));

        if (reg.isConfirmed()) {
            throw new BusinessException("Cannot reject a confirmed registration");
        }

        reg.setNoShow(true);
        reg.setCheckedOut(false);
        reg.setCheckOutAt(null);
        reg.setActualMinutes(null);
        registrationRepository.save(reg);
    }

    // ── Slot Admin ──────────────────────────────────────────────────────

    public CleaningSlotInfo updateSlot(UUID slotId, UpdateSlotRequest request) {
        CleaningSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("CleaningSlot", slotId));
        if (request.startTime() != null) slot.setStartTime(request.startTime());
        if (request.endTime() != null) slot.setEndTime(request.endTime());
        if (request.minParticipants() != null) slot.setMinParticipants(request.minParticipants());
        if (request.maxParticipants() != null) slot.setMaxParticipants(request.maxParticipants());
        slot = slotRepository.save(slot);
        return toSlotInfo(slot, getConfigTitle(slot.getConfigId()));
    }

    public void cancelSlot(UUID slotId) {
        CleaningSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("CleaningSlot", slotId));
        slot.setCancelled(true);
        slot.setStatus("CANCELLED");
        slotRepository.save(slot);
    }

    // ── Family Hours ────────────────────────────────────────────────────

    @Override
    public void linkJobToConfig(UUID configId, UUID jobId) {
        CleaningConfig config = configRepository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("CleaningConfig", configId));
        config.setJobId(jobId);
        configRepository.save(config);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCleaningHoursForFamily(UUID familyId) {
        LocalDate yearStart = LocalDate.now().withDayOfYear(1);
        LocalDate yearEnd = LocalDate.now().withMonth(12).withDayOfMonth(31);
        int totalMinutes = registrationRepository.sumActualMinutesByFamilyInRange(familyId, yearStart, yearEnd);
        return BigDecimal.valueOf(totalMinutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCleaningHoursForFamilyInRange(UUID familyId, LocalDate from, LocalDate to) {
        int totalMinutes = registrationRepository.sumActualMinutesByFamilyInRange(familyId, from, to);
        return BigDecimal.valueOf(totalMinutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    // ── Dashboard ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DashboardInfo getDashboard(UUID sectionId, LocalDate from, LocalDate to) {
        long totalSlots = slotRepository.countTotalSlots(sectionId, from, to);
        long completedSlots = slotRepository.countCompletedSlots(sectionId, from, to);
        long noShows = registrationRepository.countNoShowsBySectionInRange(sectionId, from, to);

        List<CleaningSlot> upcoming = slotRepository.findBySectionAndDateRange(
                sectionId, LocalDate.now(), to);
        int slotsNeedingMore = 0;
        for (CleaningSlot s : upcoming) {
            long regCount = registrationRepository.countBySlotId(s.getId());
            if (regCount < s.getMinParticipants()) {
                slotsNeedingMore++;
            }
        }

        return new DashboardInfo(totalSlots, completedSlots, noShows, slotsNeedingMore);
    }

    // ── QR Code ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public String getQrToken(UUID slotId) {
        CleaningSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("CleaningSlot", slotId));
        return slot.getQrToken();
    }

    // ── Config / Slot queries for PDF export ─────────────────────────────

    @Transactional(readOnly = true)
    public CleaningConfigInfo getConfigById(UUID configId) {
        CleaningConfig config = configRepository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("CleaningConfig", configId));
        return toConfigInfo(config);
    }

    @Transactional(readOnly = true)
    public List<SlotWithToken> getSlotsByConfigAndDateRange(UUID configId, LocalDate from, LocalDate to) {
        List<CleaningSlot> slots = slotRepository.findByConfigIdAndSlotDateBetween(configId, from, to);
        return slots.stream()
                .map(s -> new SlotWithToken(s.getSlotDate(), s.getStartTime(), s.getEndTime(), s.getQrToken()))
                .toList();
    }

    // ── Mapping ─────────────────────────────────────────────────────────

    private CleaningConfigInfo toConfigInfo(CleaningConfig config) {
        String sectionName = getSectionName(config.getSectionId());
        return new CleaningConfigInfo(
                config.getId(), config.getSectionId(), sectionName,
                config.getTitle(), config.getDescription(),
                config.getDayOfWeek(), config.getStartTime(), config.getEndTime(),
                config.getMinParticipants(), config.getMaxParticipants(),
                config.getHoursCredit(), config.isActive(),
                config.getSpecificDate(),
                config.getCalendarEventId(), config.getJobId());
    }

    private CleaningSlotInfo toSlotInfo(CleaningSlot slot, String configTitle) {
        String sectionName = getSectionName(slot.getSectionId());
        List<CleaningRegistration> regs = registrationRepository.findBySlotId(slot.getId());
        List<CleaningSlotInfo.RegistrationInfo> regInfos = regs.stream()
                .map(this::toRegistrationInfo)
                .toList();

        return new CleaningSlotInfo(
                slot.getId(), slot.getConfigId(), slot.getSectionId(), sectionName,
                configTitle, slot.getSlotDate(), slot.getStartTime(), slot.getEndTime(),
                slot.getMinParticipants(), slot.getMaxParticipants(),
                regs.size(), CleaningSlotStatus.valueOf(slot.getStatus()),
                slot.isCancelled(), regInfos);
    }

    private CleaningSlotInfo.RegistrationInfo toRegistrationInfo(CleaningRegistration reg) {
        return new CleaningSlotInfo.RegistrationInfo(
                reg.getId(), reg.getUserId(), reg.getUserName(), reg.getFamilyId(),
                reg.isCheckedIn(), reg.isCheckedOut(), reg.getActualMinutes(),
                reg.isNoShow(), reg.isSwapOffered(),
                reg.isConfirmed(), reg.getConfirmedBy(), reg.getConfirmedAt());
    }

    private String getSectionName(UUID sectionId) {
        try {
            return schoolModuleApi.findById(sectionId)
                    .map(SchoolSectionInfo::name)
                    .orElse("");
        } catch (Exception e) {
            return "";
        }
    }

    private String getConfigTitle(UUID configId) {
        return configRepository.findById(configId)
                .map(CleaningConfig::getTitle)
                .orElse("");
    }

    // ── Request/Response Records ────────────────────────────────────────

    public record CreateConfigRequest(
            UUID sectionId, String title, String description,
            int dayOfWeek, LocalTime startTime, LocalTime endTime,
            int minParticipants, int maxParticipants, BigDecimal hoursCredit,
            LocalDate specificDate) {
    }

    public record UpdateConfigRequest(
            String title, String description,
            LocalTime startTime, LocalTime endTime,
            Integer minParticipants, Integer maxParticipants,
            BigDecimal hoursCredit, Boolean active) {
    }

    public record UpdateSlotRequest(
            LocalTime startTime, LocalTime endTime,
            Integer minParticipants, Integer maxParticipants) {
    }

    public record GenerateSlotsRequest(LocalDate from, LocalDate to) {
    }

    public record DashboardInfo(
            long totalSlots, long completedSlots, long noShows, int slotsNeedingParticipants) {
    }

    public record SlotWithToken(LocalDate date, LocalTime startTime, LocalTime endTime, String qrToken) {
    }
}
