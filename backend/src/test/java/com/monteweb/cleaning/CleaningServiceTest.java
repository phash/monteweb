package com.monteweb.cleaning;

import com.monteweb.calendar.CalendarModuleApi;
import com.monteweb.cleaning.internal.model.CleaningConfig;
import com.monteweb.cleaning.internal.model.CleaningRegistration;
import com.monteweb.cleaning.internal.model.CleaningSlot;
import com.monteweb.cleaning.internal.repository.CleaningConfigRepository;
import com.monteweb.cleaning.internal.repository.CleaningRegistrationRepository;
import com.monteweb.cleaning.internal.repository.CleaningSlotRepository;
import com.monteweb.cleaning.internal.service.CleaningService;
import com.monteweb.cleaning.internal.service.QrTokenService;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.school.SchoolModuleApi;
import com.monteweb.school.SchoolSectionInfo;
import com.monteweb.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CleaningService covering slot generation, registration,
 * check-in, and check-out workflows.
 */
@ExtendWith(MockitoExtension.class)
class CleaningServiceTest {

    @Mock private CleaningConfigRepository configRepository;
    @Mock private CleaningSlotRepository slotRepository;
    @Mock private CleaningRegistrationRepository registrationRepository;
    @Mock private QrTokenService qrTokenService;
    @Mock private SchoolModuleApi schoolModuleApi;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private CalendarModuleApi calendarModuleApi;
    @Mock private RoomModuleApi roomModuleApi;

    private CleaningService service;

    private static final UUID CONFIG_ID = UUID.randomUUID();
    private static final UUID SECTION_ID = UUID.randomUUID();
    private static final UUID SLOT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID FAMILY_ID = UUID.randomUUID();
    private static final String USER_NAME = "Anna Mueller";
    private static final String SECTION_NAME = "Sonnengruppe";
    private static final String CONFIG_TITLE = "Freitagsputz";

    @BeforeEach
    void setUp() {
        service = new CleaningService(
                configRepository, slotRepository, registrationRepository,
                qrTokenService, schoolModuleApi, eventPublisher,
                calendarModuleApi, roomModuleApi
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private CleaningConfig makeConfig(LocalDate specificDate, Integer dayOfWeek) {
        CleaningConfig config = new CleaningConfig();
        config.setId(CONFIG_ID);
        config.setSectionId(SECTION_ID);
        config.setTitle(CONFIG_TITLE);
        config.setDescription("Wochenreinigung");
        config.setStartTime(LocalTime.of(14, 0));
        config.setEndTime(LocalTime.of(16, 0));
        config.setMinParticipants(2);
        config.setMaxParticipants(5);
        config.setHoursCredit(new BigDecimal("2.0"));
        config.setActive(true);
        config.setSpecificDate(specificDate);
        config.setDayOfWeek(dayOfWeek != null ? dayOfWeek : 5); // Friday default
        return config;
    }

    private CleaningSlot makeSlot(UUID slotId, LocalDate date, String status) {
        CleaningSlot slot = new CleaningSlot();
        slot.setId(slotId);
        slot.setConfigId(CONFIG_ID);
        slot.setSectionId(SECTION_ID);
        slot.setSlotDate(date);
        slot.setStartTime(LocalTime.of(14, 0));
        slot.setEndTime(LocalTime.of(16, 0));
        slot.setMinParticipants(2);
        slot.setMaxParticipants(5);
        slot.setStatus(status);
        slot.setCancelled(false);
        slot.setQrToken("valid-qr-token");
        return slot;
    }

    private CleaningRegistration makeRegistration(UUID slotId, UUID userId, boolean checkedIn, boolean checkedOut) {
        CleaningRegistration reg = new CleaningRegistration();
        reg.setId(UUID.randomUUID());
        reg.setSlotId(slotId);
        reg.setUserId(userId);
        reg.setUserName(USER_NAME);
        reg.setFamilyId(FAMILY_ID);
        reg.setCheckedIn(checkedIn);
        reg.setCheckedOut(checkedOut);
        if (checkedIn) {
            reg.setCheckInAt(Instant.now().minusSeconds(3600)); // 1 hour ago
        }
        if (checkedOut) {
            reg.setCheckOutAt(Instant.now());
            reg.setActualMinutes(60);
        }
        return reg;
    }

    private SchoolSectionInfo makeSectionInfo() {
        return new SchoolSectionInfo(SECTION_ID, SECTION_NAME, "sonnengruppe",
                "Kindergarten Sonnengruppe", 1, true);
    }

    private void mockSectionLookup() {
        lenient().when(schoolModuleApi.findById(SECTION_ID))
                .thenReturn(Optional.of(makeSectionInfo()));
    }

    private void mockConfigLookup() {
        lenient().when(configRepository.findById(CONFIG_ID))
                .thenReturn(Optional.of(makeConfig(null, 5)));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Generate Slots
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Generate Slots")
    class GenerateSlots {

        @Test
        @DisplayName("One-time config creates exactly one slot when specificDate is in range")
        void generateSlots_oneTimeCreatesOneSlot() {
            LocalDate specificDate = LocalDate.of(2026, 3, 6); // a Friday
            CleaningConfig config = makeConfig(specificDate, specificDate.getDayOfWeek().getValue());
            LocalDate from = LocalDate.of(2026, 3, 1);
            LocalDate to = LocalDate.of(2026, 3, 31);

            when(configRepository.findById(CONFIG_ID)).thenReturn(Optional.of(config));
            when(slotRepository.findByConfigIdAndSlotDateBetween(CONFIG_ID, from, to))
                    .thenReturn(List.of());
            when(qrTokenService.generateToken(any(UUID.class))).thenReturn("token-123");
            when(slotRepository.saveAll(anyList())).thenAnswer(inv -> {
                List<CleaningSlot> slots = inv.getArgument(0);
                slots.forEach(s -> s.setId(UUID.randomUUID()));
                return slots;
            });
            mockSectionLookup();
            when(registrationRepository.findBySlotId(any())).thenReturn(List.of());

            List<CleaningSlotInfo> result = service.generateSlots(CONFIG_ID, from, to);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).slotDate()).isEqualTo(specificDate);
            verify(slotRepository, times(2)).saveAll(anyList()); // initial save + QR regeneration
        }

        @Test
        @DisplayName("One-time config with specificDate outside range creates no slots")
        void generateSlots_outsideRangeCreatesNone() {
            LocalDate specificDate = LocalDate.of(2026, 2, 15);
            CleaningConfig config = makeConfig(specificDate, specificDate.getDayOfWeek().getValue());
            LocalDate from = LocalDate.of(2026, 3, 1);
            LocalDate to = LocalDate.of(2026, 3, 31);

            when(configRepository.findById(CONFIG_ID)).thenReturn(Optional.of(config));
            when(slotRepository.findByConfigIdAndSlotDateBetween(CONFIG_ID, from, to))
                    .thenReturn(List.of());
            when(slotRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<CleaningSlotInfo> result = service.generateSlots(CONFIG_ID, from, to);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Recurring config generates slots only for matching dayOfWeek")
        void generateSlots_recurringCorrectDays() {
            // Monday = 1
            CleaningConfig config = makeConfig(null, DayOfWeek.MONDAY.getValue());
            // Mon 2 March - Sun 8 March 2026 => one Monday (2nd)
            LocalDate from = LocalDate.of(2026, 3, 2);
            LocalDate to = LocalDate.of(2026, 3, 8);

            when(configRepository.findById(CONFIG_ID)).thenReturn(Optional.of(config));
            when(slotRepository.findByConfigIdAndSlotDateBetween(CONFIG_ID, from, to))
                    .thenReturn(List.of());
            when(qrTokenService.generateToken(any(UUID.class))).thenReturn("token-456");
            when(slotRepository.saveAll(anyList())).thenAnswer(inv -> {
                List<CleaningSlot> slots = inv.getArgument(0);
                slots.forEach(s -> s.setId(UUID.randomUUID()));
                return slots;
            });
            mockSectionLookup();
            when(registrationRepository.findBySlotId(any())).thenReturn(List.of());

            List<CleaningSlotInfo> result = service.generateSlots(CONFIG_ID, from, to);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).slotDate().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        }

        @Test
        @DisplayName("Recurring config skips dates where a slot already exists (deduplication)")
        void generateSlots_deduplicationSkipsExisting() {
            CleaningConfig config = makeConfig(null, DayOfWeek.MONDAY.getValue());
            LocalDate from = LocalDate.of(2026, 3, 2);
            LocalDate to = LocalDate.of(2026, 3, 8);

            // Monday March 2 already has a slot
            CleaningSlot existingSlot = makeSlot(UUID.randomUUID(), LocalDate.of(2026, 3, 2), "OPEN");

            when(configRepository.findById(CONFIG_ID)).thenReturn(Optional.of(config));
            when(slotRepository.findByConfigIdAndSlotDateBetween(CONFIG_ID, from, to))
                    .thenReturn(List.of(existingSlot));
            when(slotRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<CleaningSlotInfo> result = service.generateSlots(CONFIG_ID, from, to);

            assertThat(result).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Register for Slot
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Register for Slot")
    class RegisterForSlot {

        @Test
        @DisplayName("Successful registration saves and returns slot info")
        void registerForSlot_success() {
            CleaningSlot slot = makeSlot(SLOT_ID, LocalDate.of(2026, 3, 6), "OPEN");

            when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
            when(registrationRepository.existsBySlotIdAndUserId(SLOT_ID, USER_ID)).thenReturn(false);
            when(registrationRepository.countBySlotId(SLOT_ID)).thenReturn(2L);
            when(registrationRepository.save(any(CleaningRegistration.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            mockSectionLookup();
            mockConfigLookup();
            when(registrationRepository.findBySlotId(SLOT_ID)).thenReturn(List.of());

            CleaningSlotInfo result = service.registerForSlot(SLOT_ID, USER_ID, USER_NAME, FAMILY_ID);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(SLOT_ID);
            verify(registrationRepository).save(any(CleaningRegistration.class));
        }

        @Test
        @DisplayName("Registration for cancelled slot throws BusinessException")
        void registerForSlot_cancelledSlot() {
            CleaningSlot slot = makeSlot(SLOT_ID, LocalDate.of(2026, 3, 6), "OPEN");
            slot.setCancelled(true);

            when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));

            assertThatThrownBy(() -> service.registerForSlot(SLOT_ID, USER_ID, USER_NAME, FAMILY_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("cancelled");
        }

        @Test
        @DisplayName("Duplicate registration throws BusinessException")
        void registerForSlot_duplicate() {
            CleaningSlot slot = makeSlot(SLOT_ID, LocalDate.of(2026, 3, 6), "OPEN");

            when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
            when(registrationRepository.existsBySlotIdAndUserId(SLOT_ID, USER_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.registerForSlot(SLOT_ID, USER_ID, USER_NAME, FAMILY_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Already registered");
        }

        @Test
        @DisplayName("Registration when slot is full throws BusinessException")
        void registerForSlot_fullCapacity() {
            CleaningSlot slot = makeSlot(SLOT_ID, LocalDate.of(2026, 3, 6), "OPEN");
            slot.setMaxParticipants(3);

            when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
            when(registrationRepository.existsBySlotIdAndUserId(SLOT_ID, USER_ID)).thenReturn(false);
            when(registrationRepository.countBySlotId(SLOT_ID)).thenReturn(3L); // == maxParticipants

            assertThatThrownBy(() -> service.registerForSlot(SLOT_ID, USER_ID, USER_NAME, FAMILY_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("full");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Check In
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Check In")
    class CheckIn {

        @Test
        @DisplayName("Valid QR token check-in sets checkedIn and slot to IN_PROGRESS")
        void checkIn_validQrToken() {
            CleaningSlot slot = makeSlot(SLOT_ID, LocalDate.of(2026, 3, 6), "OPEN");
            CleaningRegistration reg = makeRegistration(SLOT_ID, USER_ID, false, false);

            when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
            when(qrTokenService.validateToken("valid-qr-token")).thenReturn(SLOT_ID);
            when(registrationRepository.findBySlotIdAndUserId(SLOT_ID, USER_ID))
                    .thenReturn(Optional.of(reg));
            when(registrationRepository.save(any(CleaningRegistration.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(slotRepository.save(any(CleaningSlot.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            mockSectionLookup();
            mockConfigLookup();
            when(registrationRepository.findBySlotId(SLOT_ID)).thenReturn(List.of(reg));

            CleaningSlotInfo result = service.checkIn(SLOT_ID, USER_ID, "valid-qr-token");

            assertThat(result).isNotNull();
            assertThat(reg.isCheckedIn()).isTrue();
            assertThat(reg.getCheckInAt()).isNotNull();
            assertThat(slot.getStatus()).isEqualTo("IN_PROGRESS");
        }

        @Test
        @DisplayName("Invalid QR token where slot token also mismatches throws BusinessException")
        void checkIn_invalidToken() {
            CleaningSlot slot = makeSlot(SLOT_ID, LocalDate.of(2026, 3, 6), "OPEN");
            slot.setQrToken("different-stored-token");

            when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
            when(qrTokenService.validateToken("bogus-token")).thenReturn(null);

            assertThatThrownBy(() -> service.checkIn(SLOT_ID, USER_ID, "bogus-token"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid QR token");
        }

        @Test
        @DisplayName("Already checked-in user throws BusinessException")
        void checkIn_alreadyCheckedIn() {
            CleaningSlot slot = makeSlot(SLOT_ID, LocalDate.of(2026, 3, 6), "IN_PROGRESS");
            CleaningRegistration reg = makeRegistration(SLOT_ID, USER_ID, true, false);

            when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
            when(qrTokenService.validateToken("valid-qr-token")).thenReturn(SLOT_ID);
            when(registrationRepository.findBySlotIdAndUserId(SLOT_ID, USER_ID))
                    .thenReturn(Optional.of(reg));

            assertThatThrownBy(() -> service.checkIn(SLOT_ID, USER_ID, "valid-qr-token"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Already checked in");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Check Out
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Check Out")
    class CheckOut {

        @Test
        @DisplayName("Check-out calculates duration and publishes CleaningCompletedEvent with hoursCredit")
        void checkOut_durationCalculationAndCredit() {
            CleaningSlot slot = makeSlot(SLOT_ID, LocalDate.of(2026, 3, 6), "IN_PROGRESS");
            CleaningConfig config = makeConfig(null, 5);

            CleaningRegistration reg = makeRegistration(SLOT_ID, USER_ID, true, false);
            // Set a specific check-in time for deterministic duration
            reg.setCheckInAt(Instant.now().minusSeconds(5400)); // 90 min ago

            // Another user still checked in but not out -> slot should NOT complete
            CleaningRegistration otherReg = makeRegistration(SLOT_ID, UUID.randomUUID(), true, false);

            when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
            when(registrationRepository.findBySlotIdAndUserId(SLOT_ID, USER_ID))
                    .thenReturn(Optional.of(reg));
            when(registrationRepository.save(any(CleaningRegistration.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(configRepository.findById(slot.getConfigId())).thenReturn(Optional.of(config));
            when(registrationRepository.findBySlotId(SLOT_ID))
                    .thenReturn(List.of(reg, otherReg));
            mockSectionLookup();

            CleaningSlotInfo result = service.checkOut(SLOT_ID, USER_ID);

            assertThat(result).isNotNull();
            assertThat(reg.isCheckedOut()).isTrue();
            assertThat(reg.getCheckOutAt()).isNotNull();
            // actualMinutes should be approximately 90 (allowing for test execution time)
            assertThat(reg.getActualMinutes()).isBetween(89, 91);

            // Verify event published with correct hoursCredit
            ArgumentCaptor<CleaningCompletedEvent> eventCaptor =
                    ArgumentCaptor.forClass(CleaningCompletedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            CleaningCompletedEvent event = eventCaptor.getValue();
            assertThat(event.slotId()).isEqualTo(SLOT_ID);
            assertThat(event.userId()).isEqualTo(USER_ID);
            assertThat(event.hoursCredit()).isEqualByComparingTo("2.0");
            assertThat(event.familyId()).isEqualTo(FAMILY_ID);

            // Slot should NOT be completed yet (otherReg not checked out)
            assertThat(slot.getStatus()).isEqualTo("IN_PROGRESS");
        }

        @Test
        @DisplayName("Last person checking out completes slot and marks no-shows")
        void checkOut_allCheckedOutCompletesSlot() {
            CleaningSlot slot = makeSlot(SLOT_ID, LocalDate.of(2026, 3, 6), "IN_PROGRESS");
            CleaningConfig config = makeConfig(null, 5);

            // The user checking out (checked in, not yet checked out)
            CleaningRegistration reg = makeRegistration(SLOT_ID, USER_ID, true, false);
            reg.setCheckInAt(Instant.now().minusSeconds(7200)); // 2 hours ago

            // A no-show user (registered but never checked in)
            CleaningRegistration noShowReg = makeRegistration(SLOT_ID, UUID.randomUUID(), false, false);

            when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
            when(registrationRepository.findBySlotIdAndUserId(SLOT_ID, USER_ID))
                    .thenReturn(Optional.of(reg));
            when(registrationRepository.save(any(CleaningRegistration.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(slotRepository.save(any(CleaningSlot.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(configRepository.findById(slot.getConfigId())).thenReturn(Optional.of(config));
            // After checkout, reg will be checked out (set by the service method).
            // Return both registrations. The service will check:
            //   allRegs.stream().filter(checkedIn).allMatch(checkedOut)
            // reg is checkedIn=true and after service sets checkedOut=true it matches.
            // noShowReg is checkedIn=false so it's filtered out.
            when(registrationRepository.findBySlotId(SLOT_ID))
                    .thenReturn(List.of(reg, noShowReg));
            mockSectionLookup();

            CleaningSlotInfo result = service.checkOut(SLOT_ID, USER_ID);

            assertThat(result).isNotNull();
            // Slot should be COMPLETED (only checked-in user has checked out)
            assertThat(slot.getStatus()).isEqualTo("COMPLETED");
            verify(slotRepository).save(slot);

            // No-show user should be marked
            assertThat(noShowReg.isNoShow()).isTrue();
            verify(registrationRepository).save(noShowReg);

            // Event should still be published
            verify(eventPublisher).publishEvent(any(CleaningCompletedEvent.class));
        }

        @Test
        @DisplayName("Check-out without prior check-in throws BusinessException")
        void checkOut_notCheckedIn() {
            CleaningSlot slot = makeSlot(SLOT_ID, LocalDate.of(2026, 3, 6), "IN_PROGRESS");
            CleaningRegistration reg = makeRegistration(SLOT_ID, USER_ID, false, false);

            when(slotRepository.findById(SLOT_ID)).thenReturn(Optional.of(slot));
            when(registrationRepository.findBySlotIdAndUserId(SLOT_ID, USER_ID))
                    .thenReturn(Optional.of(reg));

            assertThatThrownBy(() -> service.checkOut(SLOT_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Must check in before checking out");
        }
    }
}
