package com.monteweb.jobboard;

import com.monteweb.admin.AdminModuleApi;
import com.monteweb.admin.TenantConfigInfo;
import com.monteweb.cleaning.CleaningModuleApi;
import com.monteweb.family.FamilyInfo;
import com.monteweb.family.FamilyModuleApi;
import com.monteweb.jobboard.internal.model.Job;
import com.monteweb.jobboard.internal.model.JobAssignment;
import com.monteweb.jobboard.internal.repository.JobAssignmentRepository;
import com.monteweb.jobboard.internal.repository.JobAttachmentRepository;
import com.monteweb.jobboard.internal.repository.JobRepository;
import com.monteweb.jobboard.internal.service.JobboardService;
import com.monteweb.calendar.CalendarModuleApi;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.user.UserModuleApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-Tests für die Stundenverrechnung im JobboardService (Fix #37).
 *
 * Testet die korrekte Trennung von Normal- und Reinigungsstunden,
 * Ampelberechnung, Stundenbefreiung und den Bestätigungsworkflow.
 */
@ExtendWith(MockitoExtension.class)
class JobboardServiceHoursTest {

    @Mock private JobRepository jobRepository;
    @Mock private JobAssignmentRepository assignmentRepository;
    @Mock private JobAttachmentRepository attachmentRepository;
    @Mock private UserModuleApi userModuleApi;
    @Mock private FamilyModuleApi familyModuleApi;
    @Mock private AdminModuleApi adminModuleApi;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private CleaningModuleApi cleaningModuleApi;
    @Mock private CalendarModuleApi calendarModuleApi;
    @Mock private RoomModuleApi roomModuleApi;
    @Mock private com.monteweb.jobboard.internal.service.JobStorageService storageService;

    private JobboardService service;

    private static final UUID FAMILY_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new JobboardService(
                jobRepository, assignmentRepository, attachmentRepository,
                userModuleApi, familyModuleApi, adminModuleApi,
                eventPublisher, cleaningModuleApi, calendarModuleApi,
                roomModuleApi, storageService
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private FamilyInfo makeFamily(boolean hoursExempt) {
        return new FamilyInfo(FAMILY_ID, "Familie Müller", null, hoursExempt, true, List.of());
    }

    private TenantConfigInfo makeTenantConfig(BigDecimal targetHours, BigDecimal targetCleaningHours) {
        return new TenantConfigInfo(
                UUID.randomUUID(), "Montessori Schule", null,
                Map.of(), Map.of(),
                targetHours, targetCleaningHours,
                true, true, "BY", List.of(),
                null, false, true,
                true, "de", List.of("de", "en"), true,
                null, null, null, null,
                null, null, null, null,
                null, null, null,
                "DISABLED", null,
                false,
                // LDAP fields
                false, null, null, null,
                null, null, null, null,
                "PARENT", false, false,
                // Maintenance
                false, null,
                // ClamAV (enabled via modules)
                null, 3310,
                // Jitsi (enabled via modules)
                null,
                // WOPI (enabled via modules)
                null
        );
    }

    private void setupStandardMocks(BigDecimal targetHours, BigDecimal targetCleaningHours) {
        when(adminModuleApi.getTenantConfig())
                .thenReturn(makeTenantConfig(targetHours, targetCleaningHours));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Stundentrennung: Normal vs. Reinigung
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Stundentrennung Normal/Reinigung")
    class HoursSeparation {

        @Test
        @DisplayName("Normale Stunden fließen nur in completedHours, nicht in cleaningHours")
        void normalHours_onlyInCompletedHours() {
            when(familyModuleApi.findById(FAMILY_ID)).thenReturn(Optional.of(makeFamily(false)));
            setupStandardMocks(new BigDecimal("30"), new BigDecimal("3"));

            when(assignmentRepository.sumConfirmedNormalHoursByFamilyId(FAMILY_ID))
                    .thenReturn(new BigDecimal("5.0"));
            when(assignmentRepository.sumPendingHoursByFamilyId(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);
            when(assignmentRepository.sumConfirmedCleaningJobHoursByFamilyId(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);
            when(cleaningModuleApi.getCleaningHoursForFamily(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);

            var result = service.getFamilyHours(FAMILY_ID);

            assertThat(result).isPresent();
            var hours = result.get();
            assertThat(hours.completedHours()).isEqualByComparingTo("5.0");
            assertThat(hours.cleaningHours()).isEqualByComparingTo("0");
            assertThat(hours.totalHours()).isEqualByComparingTo("5.0");
        }

        @Test
        @DisplayName("Reinigungsstunden fließen nur in cleaningHours, nicht in completedHours")
        void cleaningHours_onlyInCleaningHours() {
            when(familyModuleApi.findById(FAMILY_ID)).thenReturn(Optional.of(makeFamily(false)));
            setupStandardMocks(new BigDecimal("30"), new BigDecimal("3"));

            when(assignmentRepository.sumConfirmedNormalHoursByFamilyId(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);
            when(assignmentRepository.sumPendingHoursByFamilyId(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);
            when(assignmentRepository.sumConfirmedCleaningJobHoursByFamilyId(FAMILY_ID))
                    .thenReturn(new BigDecimal("2.5"));
            when(cleaningModuleApi.getCleaningHoursForFamily(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);

            var result = service.getFamilyHours(FAMILY_ID);

            assertThat(result).isPresent();
            var hours = result.get();
            assertThat(hours.completedHours()).isEqualByComparingTo("0");
            assertThat(hours.cleaningHours()).isEqualByComparingTo("2.5");
            assertThat(hours.totalHours()).isEqualByComparingTo("2.5");
        }

        @Test
        @DisplayName("Normal + Reinigung + QR-Stunden werden korrekt summiert")
        void combinedHours_totalIsCorrect() {
            when(familyModuleApi.findById(FAMILY_ID)).thenReturn(Optional.of(makeFamily(false)));
            setupStandardMocks(new BigDecimal("30"), new BigDecimal("5"));

            when(assignmentRepository.sumConfirmedNormalHoursByFamilyId(FAMILY_ID))
                    .thenReturn(new BigDecimal("10"));
            when(assignmentRepository.sumPendingHoursByFamilyId(FAMILY_ID))
                    .thenReturn(new BigDecimal("3"));
            when(assignmentRepository.sumConfirmedCleaningJobHoursByFamilyId(FAMILY_ID))
                    .thenReturn(new BigDecimal("1.5"));
            when(cleaningModuleApi.getCleaningHoursForFamily(FAMILY_ID))
                    .thenReturn(new BigDecimal("0.5"));

            var result = service.getFamilyHours(FAMILY_ID);

            assertThat(result).isPresent();
            var hours = result.get();
            // completedHours = nur normale bestätigte Stunden
            assertThat(hours.completedHours()).isEqualByComparingTo("10");
            // pendingHours = unbestätigte Stunden (alle Kategorien)
            assertThat(hours.pendingHours()).isEqualByComparingTo("3");
            // cleaningHours = Reinigung-Jobs (1.5) + QR-Check-in (0.5)
            assertThat(hours.cleaningHours()).isEqualByComparingTo("2.0");
            // totalHours = completedHours + cleaningHours (NICHT pending)
            assertThat(hours.totalHours()).isEqualByComparingTo("12.0");
        }

        @Test
        @DisplayName("Reststunden werden korrekt berechnet (nicht negativ)")
        void remainingHours_neverNegative() {
            when(familyModuleApi.findById(FAMILY_ID)).thenReturn(Optional.of(makeFamily(false)));
            setupStandardMocks(new BigDecimal("10"), new BigDecimal("2"));

            // Mehr geleistet als Ziel
            when(assignmentRepository.sumConfirmedNormalHoursByFamilyId(FAMILY_ID))
                    .thenReturn(new BigDecimal("15"));
            when(assignmentRepository.sumPendingHoursByFamilyId(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);
            when(assignmentRepository.sumConfirmedCleaningJobHoursByFamilyId(FAMILY_ID))
                    .thenReturn(new BigDecimal("5"));
            when(cleaningModuleApi.getCleaningHoursForFamily(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);

            var result = service.getFamilyHours(FAMILY_ID);

            assertThat(result).isPresent();
            var hours = result.get();
            // Reststunden dürfen nie negativ sein
            assertThat(hours.remainingHours()).isEqualByComparingTo("0");
            assertThat(hours.remainingCleaningHours()).isEqualByComparingTo("0");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Ampelberechnung
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Ampelberechnung (Traffic Light)")
    class TrafficLightCalculation {

        @Test
        @DisplayName("GREEN bei >= 75% Zielerreichung")
        void greenAt75Percent() {
            when(familyModuleApi.findById(FAMILY_ID)).thenReturn(Optional.of(makeFamily(false)));
            setupStandardMocks(new BigDecimal("20"), new BigDecimal("4"));

            // 15/20 = 75% normal, 3/4 = 75% Reinigung
            when(assignmentRepository.sumConfirmedNormalHoursByFamilyId(FAMILY_ID))
                    .thenReturn(new BigDecimal("12"));
            when(assignmentRepository.sumPendingHoursByFamilyId(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);
            when(assignmentRepository.sumConfirmedCleaningJobHoursByFamilyId(FAMILY_ID))
                    .thenReturn(new BigDecimal("3"));
            when(cleaningModuleApi.getCleaningHoursForFamily(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);

            var hours = service.getFamilyHours(FAMILY_ID).orElseThrow();

            // totalHours = 12 + 3 = 15; 15/20 = 75% → GREEN
            assertThat(hours.trafficLight()).isEqualTo("GREEN");
            // cleaningHours = 3; 3/4 = 75% → GREEN
            assertThat(hours.cleaningTrafficLight()).isEqualTo("GREEN");
        }

        @Test
        @DisplayName("YELLOW bei 40-74% Zielerreichung")
        void yellowAt40To74Percent() {
            when(familyModuleApi.findById(FAMILY_ID)).thenReturn(Optional.of(makeFamily(false)));
            setupStandardMocks(new BigDecimal("20"), new BigDecimal("4"));

            // 10/20 = 50% normal, 2/4 = 50% Reinigung
            when(assignmentRepository.sumConfirmedNormalHoursByFamilyId(FAMILY_ID))
                    .thenReturn(new BigDecimal("8"));
            when(assignmentRepository.sumPendingHoursByFamilyId(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);
            when(assignmentRepository.sumConfirmedCleaningJobHoursByFamilyId(FAMILY_ID))
                    .thenReturn(new BigDecimal("2"));
            when(cleaningModuleApi.getCleaningHoursForFamily(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);

            var hours = service.getFamilyHours(FAMILY_ID).orElseThrow();

            // totalHours = 8 + 2 = 10; 10/20 = 50% → YELLOW
            assertThat(hours.trafficLight()).isEqualTo("YELLOW");
            // cleaningHours = 2; 2/4 = 50% → YELLOW
            assertThat(hours.cleaningTrafficLight()).isEqualTo("YELLOW");
        }

        @Test
        @DisplayName("RED bei < 40% Zielerreichung")
        void redBelow40Percent() {
            when(familyModuleApi.findById(FAMILY_ID)).thenReturn(Optional.of(makeFamily(false)));
            setupStandardMocks(new BigDecimal("20"), new BigDecimal("4"));

            // 2/20 = 10% normal, 0.5/4 = 12.5% Reinigung
            when(assignmentRepository.sumConfirmedNormalHoursByFamilyId(FAMILY_ID))
                    .thenReturn(new BigDecimal("2"));
            when(assignmentRepository.sumPendingHoursByFamilyId(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);
            when(assignmentRepository.sumConfirmedCleaningJobHoursByFamilyId(FAMILY_ID))
                    .thenReturn(new BigDecimal("0.5"));
            when(cleaningModuleApi.getCleaningHoursForFamily(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);

            var hours = service.getFamilyHours(FAMILY_ID).orElseThrow();

            // totalHours = 2.5; 2.5/20 = 12.5% → RED
            assertThat(hours.trafficLight()).isEqualTo("RED");
            // cleaningHours = 0.5; 0.5/4 = 12.5% → RED
            assertThat(hours.cleaningTrafficLight()).isEqualTo("RED");
        }

        @Test
        @DisplayName("GREEN wenn Ziel = 0 (kein Stundenziel konfiguriert)")
        void greenWhenTargetIsZero() {
            when(familyModuleApi.findById(FAMILY_ID)).thenReturn(Optional.of(makeFamily(false)));
            setupStandardMocks(BigDecimal.ZERO, BigDecimal.ZERO);

            when(assignmentRepository.sumConfirmedNormalHoursByFamilyId(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);
            when(assignmentRepository.sumPendingHoursByFamilyId(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);
            when(assignmentRepository.sumConfirmedCleaningJobHoursByFamilyId(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);
            when(cleaningModuleApi.getCleaningHoursForFamily(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);

            var hours = service.getFamilyHours(FAMILY_ID).orElseThrow();

            assertThat(hours.trafficLight()).isEqualTo("GREEN");
            assertThat(hours.cleaningTrafficLight()).isEqualTo("GREEN");
        }

        @Test
        @DisplayName("Getrennte Ampeln: Normal=RED, Reinigung=GREEN gleichzeitig möglich")
        void independentTrafficLights() {
            when(familyModuleApi.findById(FAMILY_ID)).thenReturn(Optional.of(makeFamily(false)));
            setupStandardMocks(new BigDecimal("30"), new BigDecimal("3"));

            // Normal: 3/30 = 10% → RED; Reinigung: 3/3 = 100% → GREEN
            when(assignmentRepository.sumConfirmedNormalHoursByFamilyId(FAMILY_ID))
                    .thenReturn(new BigDecimal("0"));
            when(assignmentRepository.sumPendingHoursByFamilyId(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);
            when(assignmentRepository.sumConfirmedCleaningJobHoursByFamilyId(FAMILY_ID))
                    .thenReturn(new BigDecimal("3"));
            when(cleaningModuleApi.getCleaningHoursForFamily(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);

            var hours = service.getFamilyHours(FAMILY_ID).orElseThrow();

            // totalHours = 0 + 3 = 3; 3/30 = 10% → RED
            assertThat(hours.trafficLight()).isEqualTo("RED");
            // cleaningHours = 3; 3/3 = 100% → GREEN
            assertThat(hours.cleaningTrafficLight()).isEqualTo("GREEN");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Stundenbefreiung (hoursExempt)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Stundenbefreiung (hoursExempt)")
    class HoursExemption {

        @Test
        @DisplayName("Befreite Familie: alle Werte auf 0, Ampel GREEN")
        void exemptFamily_allZeroAndGreen() {
            when(familyModuleApi.findById(FAMILY_ID)).thenReturn(Optional.of(makeFamily(true)));

            var result = service.getFamilyHours(FAMILY_ID);

            assertThat(result).isPresent();
            var hours = result.get();
            assertThat(hours.hoursExempt()).isTrue();
            assertThat(hours.targetHours()).isEqualByComparingTo("0");
            assertThat(hours.completedHours()).isEqualByComparingTo("0");
            assertThat(hours.pendingHours()).isEqualByComparingTo("0");
            assertThat(hours.cleaningHours()).isEqualByComparingTo("0");
            assertThat(hours.totalHours()).isEqualByComparingTo("0");
            assertThat(hours.remainingHours()).isEqualByComparingTo("0");
            assertThat(hours.trafficLight()).isEqualTo("GREEN");
            assertThat(hours.cleaningTrafficLight()).isEqualTo("GREEN");

            // Repository-Methoden dürfen NICHT aufgerufen werden
            verifyNoInteractions(assignmentRepository);
            verifyNoInteractions(adminModuleApi);
        }

        @Test
        @DisplayName("Nicht-befreite Familie: Repository-Methoden werden aufgerufen")
        void nonExemptFamily_queriesRepository() {
            when(familyModuleApi.findById(FAMILY_ID)).thenReturn(Optional.of(makeFamily(false)));
            setupStandardMocks(new BigDecimal("30"), new BigDecimal("3"));

            when(assignmentRepository.sumConfirmedNormalHoursByFamilyId(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);
            when(assignmentRepository.sumPendingHoursByFamilyId(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);
            when(assignmentRepository.sumConfirmedCleaningJobHoursByFamilyId(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);
            when(cleaningModuleApi.getCleaningHoursForFamily(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);

            service.getFamilyHours(FAMILY_ID);

            verify(assignmentRepository).sumConfirmedNormalHoursByFamilyId(FAMILY_ID);
            verify(assignmentRepository).sumConfirmedCleaningJobHoursByFamilyId(FAMILY_ID);
            verify(assignmentRepository).sumPendingHoursByFamilyId(FAMILY_ID);
            verify(adminModuleApi).getTenantConfig();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  QR-Reinigungsstunden (Cleaning Module)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("QR-Reinigungsstunden")
    class QrCleaningHours {

        @Test
        @DisplayName("QR-Check-in-Stunden werden zu Job-Reinigungsstunden addiert")
        void qrHours_addedToJobCleaningHours() {
            when(familyModuleApi.findById(FAMILY_ID)).thenReturn(Optional.of(makeFamily(false)));
            setupStandardMocks(new BigDecimal("30"), new BigDecimal("5"));

            when(assignmentRepository.sumConfirmedNormalHoursByFamilyId(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);
            when(assignmentRepository.sumPendingHoursByFamilyId(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);
            // 2h aus Reinigung-Jobs
            when(assignmentRepository.sumConfirmedCleaningJobHoursByFamilyId(FAMILY_ID))
                    .thenReturn(new BigDecimal("2"));
            // 1.5h aus QR-Check-in
            when(cleaningModuleApi.getCleaningHoursForFamily(FAMILY_ID))
                    .thenReturn(new BigDecimal("1.5"));

            var hours = service.getFamilyHours(FAMILY_ID).orElseThrow();

            // cleaningHours = Job-Reinigung (2) + QR (1.5) = 3.5
            assertThat(hours.cleaningHours()).isEqualByComparingTo("3.5");
        }

        @Test
        @DisplayName("Ohne Cleaning-Modul: nur Job-Reinigungsstunden zählen")
        void withoutCleaningModule_onlyJobHours() {
            // Service ohne CleaningModuleApi
            var serviceNoClean = new JobboardService(
                    jobRepository, assignmentRepository, attachmentRepository,
                    userModuleApi, familyModuleApi, adminModuleApi,
                    eventPublisher, null, calendarModuleApi,
                    roomModuleApi, null
            );

            when(familyModuleApi.findById(FAMILY_ID)).thenReturn(Optional.of(makeFamily(false)));
            when(adminModuleApi.getTenantConfig())
                    .thenReturn(makeTenantConfig(new BigDecimal("30"), new BigDecimal("3")));

            when(assignmentRepository.sumConfirmedNormalHoursByFamilyId(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);
            when(assignmentRepository.sumPendingHoursByFamilyId(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);
            when(assignmentRepository.sumConfirmedCleaningJobHoursByFamilyId(FAMILY_ID))
                    .thenReturn(new BigDecimal("2"));

            var hours = serviceNoClean.getFamilyHours(FAMILY_ID).orElseThrow();

            // Nur Job-Reinigungsstunden, kein QR
            assertThat(hours.cleaningHours()).isEqualByComparingTo("2");
            verifyNoInteractions(cleaningModuleApi);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Bestätigungsworkflow
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Bestätigungsworkflow (Confirm)")
    class ConfirmWorkflow {

        @Test
        @DisplayName("Bestätigte Stunden fließen in completedHours, unbestätigte in pendingHours")
        void confirmedVsPendingHours() {
            when(familyModuleApi.findById(FAMILY_ID)).thenReturn(Optional.of(makeFamily(false)));
            setupStandardMocks(new BigDecimal("30"), new BigDecimal("3"));

            // 8h bestätigt, 4h pending
            when(assignmentRepository.sumConfirmedNormalHoursByFamilyId(FAMILY_ID))
                    .thenReturn(new BigDecimal("8"));
            when(assignmentRepository.sumPendingHoursByFamilyId(FAMILY_ID))
                    .thenReturn(new BigDecimal("4"));
            when(assignmentRepository.sumConfirmedCleaningJobHoursByFamilyId(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);
            when(cleaningModuleApi.getCleaningHoursForFamily(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);

            var hours = service.getFamilyHours(FAMILY_ID).orElseThrow();

            assertThat(hours.completedHours()).isEqualByComparingTo("8");
            assertThat(hours.pendingHours()).isEqualByComparingTo("4");
            // totalHours enthält KEINE pending Stunden
            assertThat(hours.totalHours()).isEqualByComparingTo("8");
        }

        @Test
        @DisplayName("confirmAssignment setzt confirmed-Flag und publiziert Event")
        void confirmAssignment_setsFlag() {
            var assignment = new JobAssignment();
            assignment.setId(UUID.randomUUID());
            assignment.setJobId(UUID.randomUUID());
            assignment.setUserId(USER_ID);
            assignment.setFamilyId(FAMILY_ID);
            assignment.setStatus(AssignmentStatus.COMPLETED);
            assignment.setActualHours(new BigDecimal("2.5"));
            assignment.setConfirmed(false);

            var job = new Job();
            job.setId(assignment.getJobId());
            job.setTitle("Testjob");
            job.setCategory("Garten");

            when(assignmentRepository.findById(assignment.getId()))
                    .thenReturn(Optional.of(assignment));
            when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userModuleApi.findById(USER_ID))
                    .thenReturn(Optional.of(new com.monteweb.user.UserInfo(
                            USER_ID, "anna@test.de", "Anna", "Müller",
                            "Anna Müller", null, null,
                            com.monteweb.user.UserRole.PARENT,
                            Set.of(), Set.of(), true, "SYSTEM"
                    )));
            when(jobRepository.findById(assignment.getJobId()))
                    .thenReturn(Optional.of(job));

            UUID adminId = UUID.randomUUID();
            var result = service.confirmAssignment(assignment.getId(), adminId);

            assertThat(result.confirmed()).isTrue();
            assertThat(result.confirmedBy()).isEqualTo(adminId);
            assertThat(result.confirmedAt()).isNotNull();

            // Event wurde publiziert
            verify(eventPublisher).publishEvent(any(JobCompletedEvent.class));
        }

        @Test
        @DisplayName("Nicht-COMPLETED Assignment kann nicht bestätigt werden")
        void confirmAssignment_rejectsNonCompleted() {
            var assignment = new JobAssignment();
            assignment.setId(UUID.randomUUID());
            assignment.setStatus(AssignmentStatus.IN_PROGRESS);

            when(assignmentRepository.findById(assignment.getId()))
                    .thenReturn(Optional.of(assignment));

            org.junit.jupiter.api.Assertions.assertThrows(
                    com.monteweb.shared.exception.BusinessException.class,
                    () -> service.confirmAssignment(assignment.getId(), UUID.randomUUID())
            );
        }

        @Test
        @DisplayName("Bereits bestätigtes Assignment kann nicht erneut bestätigt werden")
        void confirmAssignment_rejectsAlreadyConfirmed() {
            var assignment = new JobAssignment();
            assignment.setId(UUID.randomUUID());
            assignment.setStatus(AssignmentStatus.COMPLETED);
            assignment.setConfirmed(true);

            when(assignmentRepository.findById(assignment.getId()))
                    .thenReturn(Optional.of(assignment));

            org.junit.jupiter.api.Assertions.assertThrows(
                    com.monteweb.shared.exception.BusinessException.class,
                    () -> service.confirmAssignment(assignment.getId(), UUID.randomUUID())
            );
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Randfälle
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Randfälle")
    class EdgeCases {

        @Test
        @DisplayName("Nicht existierende Familie: leeres Optional")
        void nonExistentFamily_returnsEmpty() {
            UUID unknownId = UUID.randomUUID();
            when(familyModuleApi.findById(unknownId)).thenReturn(Optional.empty());

            var result = service.getFamilyHours(unknownId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Null-Zielwerte in TenantConfig werden als 0 behandelt")
        void nullTargets_treatedAsZero() {
            when(familyModuleApi.findById(FAMILY_ID)).thenReturn(Optional.of(makeFamily(false)));
            when(adminModuleApi.getTenantConfig())
                    .thenReturn(makeTenantConfig(null, null));

            when(assignmentRepository.sumConfirmedNormalHoursByFamilyId(FAMILY_ID))
                    .thenReturn(new BigDecimal("5"));
            when(assignmentRepository.sumPendingHoursByFamilyId(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);
            when(assignmentRepository.sumConfirmedCleaningJobHoursByFamilyId(FAMILY_ID))
                    .thenReturn(new BigDecimal("1"));
            when(cleaningModuleApi.getCleaningHoursForFamily(FAMILY_ID))
                    .thenReturn(BigDecimal.ZERO);

            var hours = service.getFamilyHours(FAMILY_ID).orElseThrow();

            assertThat(hours.targetHours()).isEqualByComparingTo("0");
            assertThat(hours.targetCleaningHours()).isEqualByComparingTo("0");
            // Bei Ziel=0 → immer GREEN
            assertThat(hours.trafficLight()).isEqualTo("GREEN");
            assertThat(hours.cleaningTrafficLight()).isEqualTo("GREEN");
            // remainingHours = max(0-6, 0) = 0
            assertThat(hours.remainingHours()).isEqualByComparingTo("0");
        }
    }
}
