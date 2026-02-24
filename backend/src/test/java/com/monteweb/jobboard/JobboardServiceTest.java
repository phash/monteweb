package com.monteweb.jobboard;

import com.monteweb.admin.AdminModuleApi;
import com.monteweb.admin.TenantConfigInfo;
import com.monteweb.calendar.CalendarModuleApi;
import com.monteweb.cleaning.CleaningModuleApi;
import com.monteweb.family.FamilyInfo;
import com.monteweb.family.FamilyModuleApi;
import com.monteweb.jobboard.internal.model.Job;
import com.monteweb.jobboard.internal.model.JobAssignment;
import com.monteweb.jobboard.internal.repository.JobAssignmentRepository;
import com.monteweb.jobboard.internal.repository.JobAttachmentRepository;
import com.monteweb.jobboard.internal.repository.JobRepository;
import com.monteweb.jobboard.internal.service.JobboardService;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.exception.ForbiddenException;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit-Tests fuer die Kernoperationen im JobboardService:
 * Bewerbung (applyForJob), Abschluss (completeAssignment) und Ablehnung (rejectAssignment).
 */
@ExtendWith(MockitoExtension.class)
class JobboardServiceTest {

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

    private JobboardService service;

    private static final UUID JOB_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID FAMILY_ID = UUID.randomUUID();
    private static final UUID ASSIGNMENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new JobboardService(
                jobRepository, assignmentRepository, attachmentRepository,
                userModuleApi, familyModuleApi, adminModuleApi,
                eventPublisher, cleaningModuleApi, calendarModuleApi,
                roomModuleApi, null
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private UserInfo makeUser(UUID id, UserRole role) {
        return new UserInfo(
                id, "user@monteweb.local", "Max", "Mustermann",
                "Max Mustermann", null, null,
                role, Set.of(), Set.of(), true, "SYSTEM"
        );
    }

    private Job makeJob(UUID id, JobStatus status, int maxAssignees) {
        var job = new Job();
        job.setId(id);
        job.setTitle("Gartenarbeit");
        job.setDescription("Beete pflegen");
        job.setCategory("Garten");
        job.setEstimatedHours(new BigDecimal("3.0"));
        job.setMaxAssignees(maxAssignees);
        job.setStatus(status);
        job.setCreatedBy(UUID.randomUUID());
        return job;
    }

    private JobAssignment makeAssignment(UUID id, UUID jobId, UUID userId, UUID familyId, AssignmentStatus status) {
        var a = new JobAssignment();
        a.setId(id);
        a.setJobId(jobId);
        a.setUserId(userId);
        a.setFamilyId(familyId);
        a.setStatus(status);
        a.setConfirmed(false);
        return a;
    }

    private FamilyInfo makeFamily(UUID id) {
        return new FamilyInfo(id, "Familie Mustermann", null, false, true, List.of());
    }

    private TenantConfigInfo makeTenantConfig(boolean requireConfirmation) {
        return new TenantConfigInfo(
                UUID.randomUUID(), "Montessori Schule", null,
                Map.of(), Map.of(),
                new BigDecimal("30"), new BigDecimal("3"),
                true, true, "BY", List.of(),
                null, false, requireConfirmation,
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
                // ClamAV
                false, null, 3310,
                // Jitsi
                false, null,
                // WOPI
                false, null
        );
    }

    /**
     * Stubs the calls that toAssignmentInfo makes internally:
     * userModuleApi.findById, familyModuleApi.findById, jobRepository.findById
     */
    private void stubToAssignmentInfoDeps(UUID userId, UUID familyId, UUID jobId) {
        lenient().when(userModuleApi.findById(userId)).thenReturn(Optional.of(makeUser(userId, UserRole.PARENT)));
        lenient().when(familyModuleApi.findById(familyId)).thenReturn(Optional.of(makeFamily(familyId)));
        lenient().when(jobRepository.findById(jobId)).thenReturn(Optional.of(makeJob(jobId, JobStatus.OPEN, 3)));
        lenient().when(attachmentRepository.findByJobIdOrderByCreatedAtAsc(jobId)).thenReturn(List.of());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Apply for Job
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Apply for Job")
    class ApplyForJob {

        @Test
        @DisplayName("PARENT kann sich erfolgreich bewerben")
        void applyForJob_success() {
            var job = makeJob(JOB_ID, JobStatus.OPEN, 3);
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(userModuleApi.findById(USER_ID)).thenReturn(Optional.of(makeUser(USER_ID, UserRole.PARENT)));
            when(assignmentRepository.findByJobIdAndUserId(JOB_ID, USER_ID)).thenReturn(Optional.empty());
            when(assignmentRepository.countByJobIdAndStatusNot(JOB_ID, AssignmentStatus.CANCELLED)).thenReturn(0L);
            when(familyModuleApi.findByUserId(USER_ID)).thenReturn(List.of(makeFamily(FAMILY_ID)));
            when(assignmentRepository.save(any(JobAssignment.class))).thenAnswer(inv -> {
                JobAssignment saved = inv.getArgument(0);
                if (saved.getId() == null) saved.setId(ASSIGNMENT_ID);
                return saved;
            });
            // toAssignmentInfo stubs
            stubToAssignmentInfoDeps(USER_ID, FAMILY_ID, JOB_ID);

            var result = service.applyForJob(JOB_ID, USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(AssignmentStatus.ASSIGNED);
            assertThat(result.userId()).isEqualTo(USER_ID);
            assertThat(result.familyId()).isEqualTo(FAMILY_ID);
            verify(assignmentRepository).save(any(JobAssignment.class));
        }

        @Test
        @DisplayName("TEACHER wird abgelehnt (ForbiddenException)")
        void applyForJob_teacherBlocked() {
            var job = makeJob(JOB_ID, JobStatus.OPEN, 3);
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(userModuleApi.findById(USER_ID)).thenReturn(Optional.of(makeUser(USER_ID, UserRole.TEACHER)));

            assertThatThrownBy(() -> service.applyForJob(JOB_ID, USER_ID))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("role");
        }

        @Test
        @DisplayName("Job nicht OPEN wirft BusinessException")
        void applyForJob_jobNotOpen() {
            var job = makeJob(JOB_ID, JobStatus.ASSIGNED, 3);
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(userModuleApi.findById(USER_ID)).thenReturn(Optional.of(makeUser(USER_ID, UserRole.PARENT)));

            assertThatThrownBy(() -> service.applyForJob(JOB_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no longer open");
        }

        @Test
        @DisplayName("Doppelte Bewerbung wirft BusinessException")
        void applyForJob_duplicate() {
            var job = makeJob(JOB_ID, JobStatus.OPEN, 3);
            var existing = makeAssignment(ASSIGNMENT_ID, JOB_ID, USER_ID, FAMILY_ID, AssignmentStatus.ASSIGNED);
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(userModuleApi.findById(USER_ID)).thenReturn(Optional.of(makeUser(USER_ID, UserRole.PARENT)));
            when(assignmentRepository.findByJobIdAndUserId(JOB_ID, USER_ID)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.applyForJob(JOB_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already applied");
        }

        @Test
        @DisplayName("Job voll wirft BusinessException")
        void applyForJob_jobFull() {
            var job = makeJob(JOB_ID, JobStatus.OPEN, 2);
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(userModuleApi.findById(USER_ID)).thenReturn(Optional.of(makeUser(USER_ID, UserRole.PARENT)));
            when(assignmentRepository.findByJobIdAndUserId(JOB_ID, USER_ID)).thenReturn(Optional.empty());
            when(assignmentRepository.countByJobIdAndStatusNot(JOB_ID, AssignmentStatus.CANCELLED)).thenReturn(2L);

            assertThatThrownBy(() -> service.applyForJob(JOB_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Maximum");
        }

        @Test
        @DisplayName("Stornierte Bewerbung wird reaktiviert")
        void applyForJob_reactivateCancelled() {
            var job = makeJob(JOB_ID, JobStatus.OPEN, 3);
            var cancelled = makeAssignment(ASSIGNMENT_ID, JOB_ID, USER_ID, FAMILY_ID, AssignmentStatus.CANCELLED);
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(userModuleApi.findById(USER_ID)).thenReturn(Optional.of(makeUser(USER_ID, UserRole.PARENT)));
            when(assignmentRepository.findByJobIdAndUserId(JOB_ID, USER_ID)).thenReturn(Optional.of(cancelled));
            when(assignmentRepository.countByJobIdAndStatusNot(JOB_ID, AssignmentStatus.CANCELLED)).thenReturn(0L);
            when(assignmentRepository.save(any(JobAssignment.class))).thenAnswer(inv -> inv.getArgument(0));
            // toAssignmentInfo stubs
            stubToAssignmentInfoDeps(USER_ID, FAMILY_ID, JOB_ID);

            var result = service.applyForJob(JOB_ID, USER_ID);

            assertThat(result.status()).isEqualTo(AssignmentStatus.ASSIGNED);
            // Verify we reused the existing assignment (save called with existing ID)
            ArgumentCaptor<JobAssignment> captor = ArgumentCaptor.forClass(JobAssignment.class);
            verify(assignmentRepository).save(captor.capture());
            assertThat(captor.getValue().getId()).isEqualTo(ASSIGNMENT_ID);
        }

        @Test
        @DisplayName("Job wird ASSIGNED wenn letzer Platz belegt")
        void applyForJob_autoAssignedWhenFull() {
            var job = makeJob(JOB_ID, JobStatus.OPEN, 2);
            // toAssignmentInfo stubs (set up first so the findById override below takes precedence)
            lenient().when(userModuleApi.findById(USER_ID)).thenReturn(Optional.of(makeUser(USER_ID, UserRole.PARENT)));
            lenient().when(familyModuleApi.findById(FAMILY_ID)).thenReturn(Optional.of(makeFamily(FAMILY_ID)));
            lenient().when(attachmentRepository.findByJobIdOrderByCreatedAtAsc(JOB_ID)).thenReturn(List.of());

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(assignmentRepository.findByJobIdAndUserId(JOB_ID, USER_ID)).thenReturn(Optional.empty());
            // 1 existing assignee; max = 2; after this apply it becomes 2 → full
            when(assignmentRepository.countByJobIdAndStatusNot(JOB_ID, AssignmentStatus.CANCELLED)).thenReturn(1L);
            when(familyModuleApi.findByUserId(USER_ID)).thenReturn(List.of(makeFamily(FAMILY_ID)));
            when(assignmentRepository.save(any(JobAssignment.class))).thenAnswer(inv -> {
                JobAssignment saved = inv.getArgument(0);
                if (saved.getId() == null) saved.setId(ASSIGNMENT_ID);
                return saved;
            });
            when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

            service.applyForJob(JOB_ID, USER_ID);

            // Job status should have been saved as ASSIGNED
            ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
            verify(jobRepository).save(jobCaptor.capture());
            assertThat(jobCaptor.getValue().getStatus()).isEqualTo(JobStatus.ASSIGNED);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Complete Assignment
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Complete Assignment")
    class CompleteAssignment {

        @Test
        @DisplayName("Auto-Confirm wenn requireAssignmentConfirmation=false")
        void completeAssignment_autoConfirmWhenToggleDisabled() {
            var assignment = makeAssignment(ASSIGNMENT_ID, JOB_ID, USER_ID, FAMILY_ID, AssignmentStatus.IN_PROGRESS);
            var job = makeJob(JOB_ID, JobStatus.IN_PROGRESS, 1);

            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
            when(adminModuleApi.getTenantConfig()).thenReturn(makeTenantConfig(false));
            when(assignmentRepository.save(any(JobAssignment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userModuleApi.findById(USER_ID)).thenReturn(Optional.of(makeUser(USER_ID, UserRole.PARENT)));
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(assignmentRepository.findByJobId(JOB_ID)).thenReturn(List.of(assignment));
            lenient().when(familyModuleApi.findById(FAMILY_ID)).thenReturn(Optional.of(makeFamily(FAMILY_ID)));
            lenient().when(attachmentRepository.findByJobIdOrderByCreatedAtAsc(JOB_ID)).thenReturn(List.of());

            var result = service.completeAssignment(ASSIGNMENT_ID, USER_ID, new BigDecimal("2.5"), "Alles erledigt");

            assertThat(result.status()).isEqualTo(AssignmentStatus.COMPLETED);
            assertThat(result.confirmed()).isTrue();
            assertThat(result.actualHours()).isEqualByComparingTo("2.5");
            verify(eventPublisher).publishEvent(any(JobCompletedEvent.class));
        }

        @Test
        @DisplayName("Falscher User wirft ForbiddenException")
        void completeAssignment_wrongUser() {
            UUID otherUser = UUID.randomUUID();
            var assignment = makeAssignment(ASSIGNMENT_ID, JOB_ID, USER_ID, FAMILY_ID, AssignmentStatus.IN_PROGRESS);

            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));

            assertThatThrownBy(() -> service.completeAssignment(ASSIGNMENT_ID, otherUser, new BigDecimal("2"), null))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("assignee");
        }

        @Test
        @DisplayName("CANCELLED Status wirft BusinessException")
        void completeAssignment_wrongStatus() {
            var assignment = makeAssignment(ASSIGNMENT_ID, JOB_ID, USER_ID, FAMILY_ID, AssignmentStatus.CANCELLED);

            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));

            assertThatThrownBy(() -> service.completeAssignment(ASSIGNMENT_ID, USER_ID, new BigDecimal("1"), null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("cannot be completed");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Reject Assignment
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Reject Assignment")
    class RejectAssignment {

        @Test
        @DisplayName("COMPLETED + unconfirmed wird abgelehnt und Job wieder geoeffnet")
        void rejectAssignment_successReopensJob() {
            var assignment = makeAssignment(ASSIGNMENT_ID, JOB_ID, USER_ID, FAMILY_ID, AssignmentStatus.COMPLETED);
            assignment.setConfirmed(false);

            var job = makeJob(JOB_ID, JobStatus.ASSIGNED, 2);

            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));
            when(assignmentRepository.save(any(JobAssignment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            // After rejection: 0 active assignees < maxAssignees(2) → re-open
            when(assignmentRepository.countByJobIdAndStatusNot(JOB_ID, AssignmentStatus.CANCELLED)).thenReturn(0L);
            when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

            UUID rejecterId = UUID.randomUUID();
            service.rejectAssignment(ASSIGNMENT_ID, rejecterId);

            // Assignment should be CANCELLED
            ArgumentCaptor<JobAssignment> aCaptor = ArgumentCaptor.forClass(JobAssignment.class);
            verify(assignmentRepository).save(aCaptor.capture());
            assertThat(aCaptor.getValue().getStatus()).isEqualTo(AssignmentStatus.CANCELLED);

            // Job should be re-opened to OPEN
            ArgumentCaptor<Job> jCaptor = ArgumentCaptor.forClass(Job.class);
            verify(jobRepository).save(jCaptor.capture());
            assertThat(jCaptor.getValue().getStatus()).isEqualTo(JobStatus.OPEN);
        }

        @Test
        @DisplayName("Nicht-COMPLETED wirft BusinessException")
        void rejectAssignment_notCompletedThrows() {
            var assignment = makeAssignment(ASSIGNMENT_ID, JOB_ID, USER_ID, FAMILY_ID, AssignmentStatus.IN_PROGRESS);

            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));

            assertThatThrownBy(() -> service.rejectAssignment(ASSIGNMENT_ID, UUID.randomUUID()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Only completed");
        }

        @Test
        @DisplayName("Bereits bestaetigte Zuweisung kann nicht abgelehnt werden")
        void rejectAssignment_alreadyConfirmedThrows() {
            var assignment = makeAssignment(ASSIGNMENT_ID, JOB_ID, USER_ID, FAMILY_ID, AssignmentStatus.COMPLETED);
            assignment.setConfirmed(true);

            when(assignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(assignment));

            assertThatThrownBy(() -> service.rejectAssignment(ASSIGNMENT_ID, UUID.randomUUID()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("confirmed");
        }
    }
}
