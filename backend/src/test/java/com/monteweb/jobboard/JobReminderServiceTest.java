package com.monteweb.jobboard;

import com.monteweb.jobboard.internal.model.Job;
import com.monteweb.jobboard.internal.model.JobAssignment;
import com.monteweb.jobboard.internal.repository.BillingPeriodRepository;
import com.monteweb.jobboard.internal.repository.JobAssignmentRepository;
import com.monteweb.jobboard.internal.repository.JobRepository;
import com.monteweb.jobboard.internal.service.JobReminderService;
import com.monteweb.notification.NotificationModuleApi;
import com.monteweb.notification.NotificationType;
import com.monteweb.shared.config.EmailService;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobReminderServiceTest {

    @Mock JobRepository jobRepository;
    @Mock JobAssignmentRepository assignmentRepository;
    @Mock BillingPeriodRepository billingPeriodRepository;
    @Mock UserModuleApi userModuleApi;
    @Mock NotificationModuleApi notificationModuleApi;
    @Mock EmailService emailService;

    JobReminderService service;

    private UserInfo user(UUID id, UserRole role) {
        return new UserInfo(id, "u" + id + "@x.de", "Vorname", "Nachname", "Vorname N.",
                null, null, role, Set.of(), Set.of(), true, "AUTO");
    }

    private Job job(UUID id, String title) {
        Job j = new Job();
        j.setId(id);
        j.setTitle(title);
        j.setScheduledDate(LocalDate.now().minusDays(40));
        return j;
    }

    @BeforeEach
    void setUp() {
        service = new JobReminderService(jobRepository, assignmentRepository, billingPeriodRepository,
                userModuleApi, notificationModuleApi, emailService);
    }

    @Test
    void overdueAssignedAssignment_remindsAssignee_andSetsFlag() {
        UUID jobId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        Job j = job(jobId, "Garten umgraben");
        JobAssignment a = new JobAssignment();
        a.setId(UUID.randomUUID());
        a.setJobId(jobId);
        a.setUserId(assigneeId);
        a.setFamilyId(UUID.randomUUID());
        a.setStatus(AssignmentStatus.ASSIGNED);

        when(assignmentRepository.findOverdueAssignments(any())).thenReturn(List.of(a));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(j));
        when(jobRepository.findOrphanedOverdueJobs(any())).thenReturn(List.of());
        when(userModuleApi.findById(assigneeId)).thenReturn(Optional.of(user(assigneeId, UserRole.PARENT)));

        service.sendOverdueReminders();

        verify(notificationModuleApi).sendNotification(eq(assigneeId), eq(NotificationType.JOB_OVERDUE),
                anyString(), anyString(), anyString(), eq("JOB"), eq(jobId));
        verify(emailService).sendGenericEmail(eq("u" + assigneeId + "@x.de"), anyString(), anyString());
        ArgumentCaptor<JobAssignment> saved = ArgumentCaptor.forClass(JobAssignment.class);
        verify(assignmentRepository).save(saved.capture());
        assertThat(saved.getValue().getOverdueReminderSentAt()).isNotNull();
    }

    @Test
    void overdueCompletedUnconfirmed_remindsSuperadmins() {
        UUID jobId = UUID.randomUUID();
        Job j = job(jobId, "Putzdienst");
        JobAssignment a = new JobAssignment();
        a.setId(UUID.randomUUID());
        a.setJobId(jobId);
        a.setUserId(UUID.randomUUID());
        a.setFamilyId(UUID.randomUUID());
        a.setStatus(AssignmentStatus.COMPLETED);
        a.setConfirmed(false);
        UUID adminId = UUID.randomUUID();

        when(assignmentRepository.findOverdueAssignments(any())).thenReturn(List.of(a));
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(j));
        when(jobRepository.findOrphanedOverdueJobs(any())).thenReturn(List.of());
        when(userModuleApi.findByRole(UserRole.SUPERADMIN)).thenReturn(List.of(user(adminId, UserRole.SUPERADMIN)));

        service.sendOverdueReminders();

        verify(notificationModuleApi).sendNotification(eq(adminId), eq(NotificationType.JOB_OVERDUE),
                anyString(), anyString(), anyString(), eq("JOB"), eq(jobId));
    }

    @Test
    void yearEnd_doesNothingWhenAlreadySent() {
        var period = new com.monteweb.jobboard.internal.model.BillingPeriod();
        period.setStatus("ACTIVE");
        period.setStartDate(LocalDate.now().minusMonths(6));
        period.setEndDate(LocalDate.now().plusDays(7));
        period.setYearEndReminderSentAt(java.time.Instant.now());
        when(billingPeriodRepository.findByStatus("ACTIVE")).thenReturn(Optional.of(period));

        service.sendYearEndReminders();

        verifyNoInteractions(notificationModuleApi);
    }
}
