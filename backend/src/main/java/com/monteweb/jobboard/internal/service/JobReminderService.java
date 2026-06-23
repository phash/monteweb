package com.monteweb.jobboard.internal.service;

import com.monteweb.jobboard.AssignmentStatus;
import com.monteweb.jobboard.internal.model.BillingPeriod;
import com.monteweb.jobboard.internal.model.Job;
import com.monteweb.jobboard.internal.model.JobAssignment;
import com.monteweb.jobboard.internal.repository.BillingPeriodRepository;
import com.monteweb.jobboard.internal.repository.JobAssignmentRepository;
import com.monteweb.jobboard.internal.repository.JobRepository;
import com.monteweb.notification.NotificationModuleApi;
import com.monteweb.notification.NotificationType;
import com.monteweb.shared.config.EmailService;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Scheduled reminders for jobs that were not processed in time.
 *  - Wave 1: 4 weeks after the scheduled date, once per job/assignment.
 *  - Wave 2: 2 weeks before the active billing period ends, once per period.
 * In-app notification always; email only when EmailService is active (monteweb.email.enabled).
 */
@Service
@ConditionalOnProperty(prefix = "monteweb.modules.jobboard", name = "enabled", havingValue = "true")
public class JobReminderService {

    private static final Logger log = LoggerFactory.getLogger(JobReminderService.class);
    private static final int OVERDUE_DAYS = 28;
    private static final int YEAR_END_LEAD_DAYS = 14;

    private final JobRepository jobRepository;
    private final JobAssignmentRepository assignmentRepository;
    private final BillingPeriodRepository billingPeriodRepository;
    private final UserModuleApi userModuleApi;
    private final NotificationModuleApi notificationModuleApi;
    private final EmailService emailService;

    public JobReminderService(JobRepository jobRepository,
                              JobAssignmentRepository assignmentRepository,
                              BillingPeriodRepository billingPeriodRepository,
                              UserModuleApi userModuleApi,
                              NotificationModuleApi notificationModuleApi,
                              @Autowired(required = false) EmailService emailService) {
        this.jobRepository = jobRepository;
        this.assignmentRepository = assignmentRepository;
        this.billingPeriodRepository = billingPeriodRepository;
        this.userModuleApi = userModuleApi;
        this.notificationModuleApi = notificationModuleApi;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 30 7 * * *")
    @Transactional
    public void sendOverdueReminders() {
        LocalDate cutoff = LocalDate.now().minusDays(OVERDUE_DAYS);

        List<JobAssignment> overdue = assignmentRepository.findOverdueAssignments(cutoff);
        for (JobAssignment a : overdue) {
            try {
                Job job = jobRepository.findById(a.getJobId()).orElse(null);
                if (job == null) continue;
                remindForAssignment(job, a);
                a.setOverdueReminderSentAt(Instant.now());
                assignmentRepository.save(a);
            } catch (Exception e) {
                log.error("Overdue reminder failed for assignment {}: {}", a.getId(), e.getMessage(), e);
            }
        }

        List<Job> orphaned = jobRepository.findOrphanedOverdueJobs(cutoff);
        for (Job job : orphaned) {
            try {
                remindForOrphanedJob(job);
                job.setOverdueReminderSentAt(Instant.now());
                jobRepository.save(job);
            } catch (Exception e) {
                log.error("Overdue reminder failed for orphaned job {}: {}", job.getId(), e.getMessage(), e);
            }
        }

        if (!overdue.isEmpty() || !orphaned.isEmpty()) {
            log.info("Job overdue reminders: {} assignment(s), {} orphaned job(s)", overdue.size(), orphaned.size());
        }
    }

    @Scheduled(cron = "0 35 7 * * *")
    @Transactional
    public void sendYearEndReminders() {
        BillingPeriod period = billingPeriodRepository.findByStatus("ACTIVE").orElse(null);
        if (period == null || period.getEndDate() == null || period.getYearEndReminderSentAt() != null) {
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDate windowStart = period.getEndDate().minusDays(YEAR_END_LEAD_DAYS);
        if (today.isBefore(windowStart) || today.isAfter(period.getEndDate())) {
            return;
        }

        List<JobAssignment> outstanding =
                assignmentRepository.findOutstandingAssignmentsInRange(period.getStartDate(), period.getEndDate());
        for (JobAssignment a : outstanding) {
            try {
                Job job = jobRepository.findById(a.getJobId()).orElse(null);
                if (job != null) remindForAssignment(job, a);
            } catch (Exception e) {
                log.error("Year-end reminder failed for assignment {}: {}", a.getId(), e.getMessage(), e);
            }
        }

        List<Job> orphaned = jobRepository.findOrphanedJobsInRange(period.getStartDate(), period.getEndDate());
        for (Job job : orphaned) {
            try {
                remindForOrphanedJob(job);
            } catch (Exception e) {
                log.error("Year-end reminder failed for orphaned job {}: {}", job.getId(), e.getMessage(), e);
            }
        }

        period.setYearEndReminderSentAt(Instant.now());
        billingPeriodRepository.save(period);
        log.info("Year-end reminders for period '{}': {} assignment(s), {} orphaned job(s)",
                period.getName(), outstanding.size(), orphaned.size());
    }

    private void remindForAssignment(Job job, JobAssignment a) {
        boolean awaitingConfirmation = a.getStatus() == AssignmentStatus.COMPLETED && !a.isConfirmed();
        if (awaitingConfirmation) {
            String title = "Job wartet auf Bestätigung";
            String message = "Der Job \"" + job.getTitle()
                    + "\" wurde als erledigt gemeldet und wartet auf Bestätigung.";
            for (UserInfo admin : userModuleApi.findByRole(UserRole.SUPERADMIN)) {
                notify(admin, title, message, job.getId());
            }
        } else {
            String title = "Job noch nicht abgeschlossen";
            String message = "Bitte schließe den Job \"" + job.getTitle()
                    + "\" ab und trage deine Stunden ein.";
            userModuleApi.findById(a.getUserId()).ifPresent(user -> notify(user, title, message, job.getId()));
        }
    }

    private void remindForOrphanedJob(Job job) {
        String title = "Job blieb liegen";
        String message = "Der Job \"" + job.getTitle()
                + "\" hat keine Bearbeiter und der Termin ist vorbei. Bitte verlängern, schließen oder neu ausschreiben.";
        List<UserInfo> recipients = new ArrayList<>(userModuleApi.findByRole(UserRole.SUPERADMIN));
        if (job.getCreatedBy() != null
                && recipients.stream().noneMatch(u -> u.id().equals(job.getCreatedBy()))) {
            userModuleApi.findById(job.getCreatedBy()).ifPresent(recipients::add);
        }
        for (UserInfo r : recipients) {
            notify(r, title, message, job.getId());
        }
    }

    private void notify(UserInfo user, String title, String message, UUID jobId) {
        notificationModuleApi.sendNotification(user.id(), NotificationType.JOB_OVERDUE,
                title, message, "/jobs/" + jobId, "JOB", jobId);
        if (emailService != null && user.email() != null) {
            emailService.sendGenericEmail(user.email(), "MonteWeb - " + title,
                    "Hallo " + user.firstName() + ",\n\n" + message
                            + "\n\nMit freundlichen Grüßen,\nIhr MonteWeb-Team");
        }
    }
}
