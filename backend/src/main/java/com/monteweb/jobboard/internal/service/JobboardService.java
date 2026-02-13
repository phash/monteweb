package com.monteweb.jobboard.internal.service;

import com.monteweb.admin.AdminModuleApi;
import com.monteweb.calendar.CalendarModuleApi;
import com.monteweb.cleaning.CleaningModuleApi;
import com.monteweb.family.FamilyInfo;
import com.monteweb.family.FamilyModuleApi;
import com.monteweb.jobboard.*;
import com.monteweb.jobboard.internal.model.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.monteweb.jobboard.internal.model.JobAssignment;
import com.monteweb.jobboard.internal.repository.JobAssignmentRepository;
import com.monteweb.jobboard.internal.repository.JobRepository;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.exception.ForbiddenException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import com.monteweb.user.UserModuleApi;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
@ConditionalOnProperty(prefix = "monteweb.modules.jobboard", name = "enabled", havingValue = "true")
public class JobboardService implements JobboardModuleApi {

    private final JobRepository jobRepository;
    private final JobAssignmentRepository assignmentRepository;
    private final UserModuleApi userModuleApi;
    private final FamilyModuleApi familyModuleApi;
    private final AdminModuleApi adminModuleApi;
    private final ApplicationEventPublisher eventPublisher;
    private final CleaningModuleApi cleaningModuleApi;
    private final CalendarModuleApi calendarModuleApi;

    public JobboardService(JobRepository jobRepository,
                           JobAssignmentRepository assignmentRepository,
                           UserModuleApi userModuleApi,
                           FamilyModuleApi familyModuleApi,
                           AdminModuleApi adminModuleApi,
                           ApplicationEventPublisher eventPublisher,
                           @Autowired(required = false) CleaningModuleApi cleaningModuleApi,
                           @Autowired(required = false) CalendarModuleApi calendarModuleApi) {
        this.jobRepository = jobRepository;
        this.assignmentRepository = assignmentRepository;
        this.userModuleApi = userModuleApi;
        this.familyModuleApi = familyModuleApi;
        this.adminModuleApi = adminModuleApi;
        this.eventPublisher = eventPublisher;
        this.cleaningModuleApi = cleaningModuleApi;
        this.calendarModuleApi = calendarModuleApi;
    }

    // ---- Public API (JobboardModuleApi) ----

    @Override
    @Transactional(readOnly = true)
    public Optional<FamilyHoursInfo> getFamilyHours(UUID familyId) {
        return familyModuleApi.findById(familyId)
                .map(this::buildFamilyHoursInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getConfirmedHoursForFamily(UUID familyId) {
        return assignmentRepository.sumConfirmedHoursByFamilyId(familyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobInfo> getJobsForEvent(UUID eventId) {
        return jobRepository.findByEventId(eventId).stream()
                .map(this::toJobInfo)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public int countJobsForEvent(UUID eventId) {
        return jobRepository.countByEventId(eventId);
    }

    // ---- Job CRUD ----

    public JobInfo createJob(UUID userId, CreateJobRequest request) {
        var job = new Job();
        job.setTitle(request.title());
        job.setDescription(request.description());
        job.setCategory(request.category());
        job.setLocation(request.location());
        job.setSectionId(request.sectionId());
        job.setEstimatedHours(request.estimatedHours());
        job.setMaxAssignees(request.maxAssignees());
        job.setScheduledDate(request.scheduledDate());
        job.setScheduledTime(request.scheduledTime());
        job.setCreatedBy(userId);
        job.setContactInfo(request.contactInfo());
        job.setStatus(JobStatus.OPEN);

        if (request.eventId() != null) {
            if (calendarModuleApi != null) {
                calendarModuleApi.findById(request.eventId())
                        .orElseThrow(() -> new BusinessException("Event not found"));
            }
            job.setEventId(request.eventId());
        }

        return toJobInfo(jobRepository.save(job));
    }

    public JobInfo updateJob(UUID jobId, UUID userId, UpdateJobRequest request) {
        var job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));

        if (!job.getCreatedBy().equals(userId)) {
            var user = userModuleApi.findById(userId);
            if (user.isEmpty() || (user.get().role() != com.monteweb.user.UserRole.SUPERADMIN
                    && user.get().role() != com.monteweb.user.UserRole.SECTION_ADMIN)) {
                throw new ForbiddenException("Only the creator or administrators can edit this job");
            }
        }

        if (request.title() != null) job.setTitle(request.title());
        if (request.description() != null) job.setDescription(request.description());
        if (request.category() != null) job.setCategory(request.category());
        if (request.location() != null) job.setLocation(request.location());
        if (request.estimatedHours() != null) job.setEstimatedHours(request.estimatedHours());
        if (request.scheduledDate() != null) job.setScheduledDate(request.scheduledDate());
        if (request.scheduledTime() != null) job.setScheduledTime(request.scheduledTime());
        if (request.contactInfo() != null) job.setContactInfo(request.contactInfo());

        return toJobInfo(jobRepository.save(job));
    }

    @Transactional
    public JobInfo linkJobToEvent(UUID jobId, UUID eventId, UUID userId) {
        var job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));
        if (!job.getCreatedBy().equals(userId)) {
            var user = userModuleApi.findById(userId)
                    .orElseThrow(() -> new ForbiddenException("User not found"));
            boolean allowed = user.role() == com.monteweb.user.UserRole.SUPERADMIN
                    || user.role() == com.monteweb.user.UserRole.SECTION_ADMIN
                    || user.role() == com.monteweb.user.UserRole.TEACHER
                    || (user.specialRoles() != null && user.specialRoles().stream()
                        .anyMatch(r -> r.startsWith("ELTERNBEIRAT")));
            // Also allow the event creator to link jobs
            if (!allowed && calendarModuleApi != null) {
                var event = calendarModuleApi.findById(eventId);
                if (event.isPresent() && event.get().createdBy().equals(userId)) {
                    allowed = true;
                }
            }
            if (!allowed) {
                throw new ForbiddenException("Only the creator, teachers, Elternbeirat, or admins can link jobs");
            }
        }
        job.setEventId(eventId);
        return toJobInfo(jobRepository.save(job));
    }

    @Transactional(readOnly = true)
    public JobInfo getJob(UUID jobId) {
        return jobRepository.findById(jobId)
                .map(this::toJobInfo)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));
    }

    @Transactional(readOnly = true)
    public Page<JobInfo> listJobs(String category, List<JobStatus> statuses, UUID eventId, Pageable pageable) {
        if (statuses == null || statuses.isEmpty()) {
            statuses = List.of(JobStatus.OPEN, JobStatus.ASSIGNED, JobStatus.IN_PROGRESS);
        }
        if (eventId != null && category != null && !category.isBlank()) {
            return jobRepository.findByCategoryAndEventIdAndStatusInOrderByScheduledDateAscCreatedAtDesc(
                    category, eventId, statuses, pageable).map(this::toJobInfo);
        }
        if (eventId != null) {
            return jobRepository.findByEventIdAndStatusInOrderByScheduledDateAscCreatedAtDesc(
                    eventId, statuses, pageable).map(this::toJobInfo);
        }
        if (category != null && !category.isBlank()) {
            return jobRepository.findByCategoryAndStatusInOrderByScheduledDateAscCreatedAtDesc(
                    category, statuses, pageable).map(this::toJobInfo);
        }
        return jobRepository.findByStatusInOrderByScheduledDateAscCreatedAtDesc(
                statuses, pageable).map(this::toJobInfo);
    }

    @Transactional(readOnly = true)
    public Page<JobInfo> listMyJobs(UUID userId, Pageable pageable) {
        return jobRepository.findByCreatedByOrderByCreatedAtDesc(userId, pageable)
                .map(this::toJobInfo);
    }

    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return jobRepository.findAllCategories();
    }

    public void cancelJob(UUID jobId, UUID userId) {
        var job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));
        // Creator can cancel; SECTION_ADMIN and SUPERADMIN can cancel any job
        if (!job.getCreatedBy().equals(userId)) {
            var user = userModuleApi.findById(userId);
            if (user.isEmpty() || (user.get().role() != com.monteweb.user.UserRole.SUPERADMIN
                    && user.get().role() != com.monteweb.user.UserRole.SECTION_ADMIN)) {
                throw new ForbiddenException("Only the creator or administrators can cancel this job");
            }
        }
        job.setStatus(JobStatus.CANCELLED);
        job.setClosedAt(Instant.now());
        jobRepository.save(job);
    }

    public void deleteJob(UUID jobId, UUID userId) {
        var job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));
        // Only creator, SUPERADMIN or SECTION_ADMIN can permanently delete
        if (!job.getCreatedBy().equals(userId)) {
            var user = userModuleApi.findById(userId);
            if (user.isEmpty() || (user.get().role() != com.monteweb.user.UserRole.SUPERADMIN
                    && user.get().role() != com.monteweb.user.UserRole.SECTION_ADMIN)) {
                throw new ForbiddenException("Only the creator or administrators can delete this job");
            }
        }
        assignmentRepository.deleteByJobId(jobId);
        jobRepository.delete(job);
    }

    // ---- Assignment operations ----

    public JobAssignmentInfo applyForJob(UUID jobId, UUID userId) {
        var job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));

        // SUPERADMIN, SECTION_ADMIN, TEACHER do not perform parent hours
        var user = userModuleApi.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
        if (user.role() == com.monteweb.user.UserRole.SUPERADMIN
                || user.role() == com.monteweb.user.UserRole.SECTION_ADMIN
                || user.role() == com.monteweb.user.UserRole.TEACHER) {
            throw new ForbiddenException("This role does not perform parent hours");
        }

        if (job.getStatus() != JobStatus.OPEN) {
            throw new BusinessException("This job is no longer open for applications");
        }

        if (assignmentRepository.existsByJobIdAndUserId(jobId, userId)) {
            throw new BusinessException("You have already applied for this job");
        }

        long currentAssignees = assignmentRepository.countByJobIdAndStatusNot(jobId, AssignmentStatus.CANCELLED);
        if (currentAssignees >= job.getMaxAssignees()) {
            throw new BusinessException("Maximum number of assignees reached");
        }

        // Find user's family
        var families = familyModuleApi.findByUserId(userId);
        if (families.isEmpty()) {
            throw new BusinessException("You must belong to a family to apply for jobs");
        }
        UUID familyId = families.get(0).id();

        var assignment = new JobAssignment();
        assignment.setJobId(jobId);
        assignment.setUserId(userId);
        assignment.setFamilyId(familyId);
        assignment.setStatus(AssignmentStatus.ASSIGNED);
        assignment = assignmentRepository.save(assignment);

        // Update job status if fully assigned
        if (currentAssignees + 1 >= job.getMaxAssignees()) {
            job.setStatus(JobStatus.ASSIGNED);
            jobRepository.save(job);
        }

        return toAssignmentInfo(assignment);
    }

    public JobAssignmentInfo startAssignment(UUID assignmentId, UUID userId) {
        var assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", assignmentId));

        if (!assignment.getUserId().equals(userId)) {
            throw new ForbiddenException("Only the assignee can start this assignment");
        }
        if (assignment.getStatus() != AssignmentStatus.ASSIGNED) {
            throw new BusinessException("Assignment is not in ASSIGNED status");
        }

        assignment.setStatus(AssignmentStatus.IN_PROGRESS);
        assignment.setStartedAt(Instant.now());
        assignment = assignmentRepository.save(assignment);

        // Update job status
        var job = jobRepository.findById(assignment.getJobId()).orElseThrow();
        if (job.getStatus() == JobStatus.ASSIGNED) {
            job.setStatus(JobStatus.IN_PROGRESS);
            jobRepository.save(job);
        }

        return toAssignmentInfo(assignment);
    }

    public JobAssignmentInfo completeAssignment(UUID assignmentId, UUID userId, BigDecimal actualHours, String notes) {
        var assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", assignmentId));

        if (!assignment.getUserId().equals(userId)) {
            throw new ForbiddenException("Only the assignee can complete this assignment");
        }
        if (assignment.getStatus() != AssignmentStatus.IN_PROGRESS && assignment.getStatus() != AssignmentStatus.ASSIGNED) {
            throw new BusinessException("Assignment cannot be completed from current status");
        }

        assignment.setStatus(AssignmentStatus.COMPLETED);
        assignment.setActualHours(actualHours);
        assignment.setNotes(notes);
        assignment.setCompletedAt(Instant.now());
        assignment = assignmentRepository.save(assignment);

        // Check if all assignments for this job are completed
        checkAndCloseJob(assignment.getJobId());

        return toAssignmentInfo(assignment);
    }

    public JobAssignmentInfo confirmAssignment(UUID assignmentId, UUID confirmerId) {
        var assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", assignmentId));

        if (assignment.getStatus() != AssignmentStatus.COMPLETED) {
            throw new BusinessException("Only completed assignments can be confirmed");
        }
        if (assignment.isConfirmed()) {
            throw new BusinessException("Assignment is already confirmed");
        }

        assignment.setConfirmed(true);
        assignment.setConfirmedBy(confirmerId);
        assignment.setConfirmedAt(Instant.now());
        assignment = assignmentRepository.save(assignment);

        // Publish event for notification
        String userName = userModuleApi.findById(assignment.getUserId())
                .map(u -> u.firstName() + " " + u.lastName())
                .orElse("Unknown");
        var job = jobRepository.findById(assignment.getJobId()).orElseThrow();

        eventPublisher.publishEvent(new JobCompletedEvent(
                job.getId(),
                job.getTitle(),
                assignment.getUserId(),
                userName,
                assignment.getFamilyId(),
                assignment.getActualHours()
        ));

        return toAssignmentInfo(assignment);
    }

    public void cancelAssignment(UUID assignmentId, UUID userId) {
        var assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", assignmentId));

        if (!assignment.getUserId().equals(userId)) {
            throw new ForbiddenException("Only the assignee can cancel this assignment");
        }
        if (assignment.getStatus() == AssignmentStatus.COMPLETED && assignment.isConfirmed()) {
            throw new BusinessException("Cannot cancel a confirmed assignment");
        }

        assignment.setStatus(AssignmentStatus.CANCELLED);
        assignmentRepository.save(assignment);

        // Re-open job if needed
        var job = jobRepository.findById(assignment.getJobId()).orElseThrow();
        long activeAssignees = assignmentRepository.countByJobIdAndStatusNot(job.getId(), AssignmentStatus.CANCELLED);
        if (activeAssignees < job.getMaxAssignees() && job.getStatus() != JobStatus.COMPLETED && job.getStatus() != JobStatus.CANCELLED) {
            job.setStatus(JobStatus.OPEN);
            jobRepository.save(job);
        }
    }

    @Transactional(readOnly = true)
    public List<JobAssignmentInfo> getAssignmentsForJob(UUID jobId) {
        return assignmentRepository.findByJobId(jobId).stream()
                .map(this::toAssignmentInfo)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<JobAssignmentInfo> getMyAssignments(UUID userId) {
        return assignmentRepository.findByUserId(userId).stream()
                .map(this::toAssignmentInfo)
                .toList();
    }

    // ---- Reporting ----

    @Transactional(readOnly = true)
    public List<FamilyHoursInfo> getAllFamilyHoursReport() {
        // Collect family IDs from job assignments
        Set<UUID> familyIds = new LinkedHashSet<>(assignmentRepository.findAllFamilyIdsWithAssignments());

        // Also include all families (they may have cleaning hours only)
        for (FamilyInfo family : familyModuleApi.findAll()) {
            familyIds.add(family.id());
        }

        List<FamilyHoursInfo> report = new ArrayList<>();
        for (UUID familyId : familyIds) {
            familyModuleApi.findById(familyId)
                    .map(this::buildFamilyHoursInfo)
                    .ifPresent(report::add);
        }

        // Sort: red first, then yellow, then green
        report.sort((a, b) -> {
            int priority = trafficLightPriority(a.trafficLight()) - trafficLightPriority(b.trafficLight());
            if (priority != 0) return priority;
            return a.familyName().compareToIgnoreCase(b.familyName());
        });

        return report;
    }

    @Transactional(readOnly = true)
    public ReportSummary getReportSummary() {
        long openJobs = jobRepository.countByStatus(JobStatus.OPEN);
        long activeJobs = jobRepository.countByStatus(JobStatus.IN_PROGRESS);
        long completedJobs = jobRepository.countByStatus(JobStatus.COMPLETED);

        var allHours = getAllFamilyHoursReport();
        long greenCount = allHours.stream().filter(h -> "GREEN".equals(h.trafficLight())).count();
        long yellowCount = allHours.stream().filter(h -> "YELLOW".equals(h.trafficLight())).count();
        long redCount = allHours.stream().filter(h -> "RED".equals(h.trafficLight())).count();

        return new ReportSummary(openJobs, activeJobs, completedJobs, greenCount, yellowCount, redCount);
    }

    // ---- Helpers ----

    private void checkAndCloseJob(UUID jobId) {
        var assignments = assignmentRepository.findByJobId(jobId);
        boolean allCompleted = assignments.stream()
                .filter(a -> a.getStatus() != AssignmentStatus.CANCELLED)
                .allMatch(a -> a.getStatus() == AssignmentStatus.COMPLETED);

        if (allCompleted && !assignments.isEmpty()) {
            var job = jobRepository.findById(jobId).orElseThrow();
            job.setStatus(JobStatus.COMPLETED);
            job.setClosedAt(Instant.now());
            jobRepository.save(job);
        }
    }

    private FamilyHoursInfo buildFamilyHoursInfo(FamilyInfo family) {
        var tenantConfig = adminModuleApi.getTenantConfig();
        BigDecimal targetHours = tenantConfig.targetHoursPerFamily();
        if (targetHours == null) targetHours = BigDecimal.ZERO;
        BigDecimal targetCleaningHrs = tenantConfig.targetCleaningHours();
        if (targetCleaningHrs == null) targetCleaningHrs = BigDecimal.ZERO;

        BigDecimal confirmed = assignmentRepository.sumConfirmedHoursByFamilyId(family.id());
        BigDecimal pending = assignmentRepository.sumPendingHoursByFamilyId(family.id());

        BigDecimal cleaningHrs = BigDecimal.ZERO;
        if (cleaningModuleApi != null) {
            cleaningHrs = cleaningModuleApi.getCleaningHoursForFamily(family.id());
        }

        BigDecimal totalHours = confirmed.add(cleaningHrs);
        BigDecimal remaining = targetHours.subtract(totalHours).max(BigDecimal.ZERO);
        BigDecimal remainingCleaningHrs = targetCleaningHrs.subtract(cleaningHrs).max(BigDecimal.ZERO);

        String trafficLight = calculateTrafficLight(totalHours, targetHours);
        String cleaningTrafficLight = calculateTrafficLight(cleaningHrs, targetCleaningHrs);

        return new FamilyHoursInfo(
                family.id(),
                family.name(),
                targetHours,
                confirmed,
                pending,
                cleaningHrs,
                totalHours,
                remaining,
                trafficLight,
                targetCleaningHrs,
                remainingCleaningHrs,
                cleaningTrafficLight
        );
    }

    private String calculateTrafficLight(BigDecimal completed, BigDecimal target) {
        if (target.compareTo(BigDecimal.ZERO) == 0) return "GREEN";

        BigDecimal percentage = completed.divide(target, 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        if (percentage.compareTo(BigDecimal.valueOf(75)) >= 0) return "GREEN";
        if (percentage.compareTo(BigDecimal.valueOf(40)) >= 0) return "YELLOW";
        return "RED";
    }

    private int trafficLightPriority(String light) {
        return switch (light) {
            case "RED" -> 0;
            case "YELLOW" -> 1;
            case "GREEN" -> 2;
            default -> 3;
        };
    }

    private JobInfo toJobInfo(Job job) {
        long currentAssignees = assignmentRepository.countByJobIdAndStatusNot(job.getId(), AssignmentStatus.CANCELLED);

        String creatorName = userModuleApi.findById(job.getCreatedBy())
                .map(u -> u.firstName() + " " + u.lastName())
                .orElse("Unknown");

        String eventTitle = null;
        if (job.getEventId() != null && calendarModuleApi != null) {
            eventTitle = calendarModuleApi.findById(job.getEventId())
                    .map(e -> e.title())
                    .orElse(null);
        }

        return new JobInfo(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getCategory(),
                job.getLocation(),
                job.getSectionId(),
                job.getEstimatedHours(),
                job.getMaxAssignees(),
                (int) currentAssignees,
                job.getStatus(),
                job.getScheduledDate(),
                job.getScheduledTime(),
                job.getCreatedBy(),
                creatorName,
                job.getContactInfo(),
                job.getEventId(),
                eventTitle,
                job.getCreatedAt()
        );
    }

    private JobAssignmentInfo toAssignmentInfo(JobAssignment a) {
        String userName = userModuleApi.findById(a.getUserId())
                .map(u -> u.firstName() + " " + u.lastName())
                .orElse("Unknown");

        String familyName = familyModuleApi.findById(a.getFamilyId())
                .map(FamilyInfo::name)
                .orElse("Unknown");

        String jobTitle = jobRepository.findById(a.getJobId())
                .map(Job::getTitle)
                .orElse("Unknown");

        return new JobAssignmentInfo(
                a.getId(),
                a.getJobId(),
                jobTitle,
                a.getUserId(),
                userName,
                a.getFamilyId(),
                familyName,
                a.getStatus(),
                a.getActualHours(),
                a.isConfirmed(),
                a.getConfirmedBy(),
                a.getConfirmedAt(),
                a.getNotes(),
                a.getAssignedAt(),
                a.getCompletedAt()
        );
    }

    // ---- Request DTOs ----

    public record CreateJobRequest(
            String title,
            String description,
            String category,
            String location,
            UUID sectionId,
            BigDecimal estimatedHours,
            int maxAssignees,
            java.time.LocalDate scheduledDate,
            String scheduledTime,
            String contactInfo,
            UUID eventId
    ) {
    }

    public record UpdateJobRequest(
            String title,
            String description,
            String category,
            String location,
            BigDecimal estimatedHours,
            java.time.LocalDate scheduledDate,
            String scheduledTime,
            String contactInfo
    ) {
    }

    public record CompleteAssignmentRequest(
            BigDecimal actualHours,
            String notes
    ) {
    }

    public record ReportSummary(
            long openJobs,
            long activeJobs,
            long completedJobs,
            long greenFamilies,
            long yellowFamilies,
            long redFamilies
    ) {
    }
}
