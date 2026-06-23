package com.monteweb.jobboard;

import com.monteweb.TestContainerConfig;
import com.monteweb.jobboard.internal.model.Job;
import com.monteweb.jobboard.internal.model.JobAssignment;
import com.monteweb.jobboard.internal.repository.JobAssignmentRepository;
import com.monteweb.jobboard.internal.repository.JobRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestContainerConfig.class)
@Transactional
class JobReminderQueriesTest {

    @Autowired JobRepository jobRepository;
    @Autowired JobAssignmentRepository assignmentRepository;
    @Autowired EntityManager em;

    private Job job(LocalDate scheduledDate, JobStatus status) {
        em.createNativeQuery("SET LOCAL session_replication_role = 'replica'").executeUpdate();
        Job j = new Job();
        j.setTitle("Test job");
        j.setCategory("Allgemein");
        j.setEstimatedHours(new BigDecimal("1.00"));
        j.setMaxAssignees(1);
        j.setStatus(status);
        j.setScheduledDate(scheduledDate);
        j.setVisibility(JobVisibility.PUBLIC);
        return jobRepository.save(j);
    }

    @Test
    void findOrphanedOverdueJobs_returnsOpenJobsPastCutoffWithoutAssignments() {
        LocalDate today = LocalDate.now();
        Job orphan = job(today.minusDays(30), JobStatus.OPEN);
        job(today.minusDays(2), JobStatus.OPEN); // not yet overdue (cutoff = today-28)

        List<Job> result = jobRepository.findOrphanedOverdueJobs(today.minusDays(28));

        assertThat(result).extracting(Job::getId).contains(orphan.getId());
    }

    @Test
    void findOrphanedOverdueJobs_excludesDraftJobs() {
        LocalDate today = LocalDate.now();
        em.createNativeQuery("SET LOCAL session_replication_role = 'replica'").executeUpdate();
        Job draft = new Job();
        draft.setTitle("Draft job");
        draft.setCategory("Allgemein");
        draft.setEstimatedHours(new BigDecimal("1.00"));
        draft.setMaxAssignees(1);
        draft.setStatus(JobStatus.OPEN);
        draft.setScheduledDate(today.minusDays(30));
        draft.setVisibility(JobVisibility.DRAFT);
        UUID draftId = jobRepository.save(draft).getId();

        List<Job> result = jobRepository.findOrphanedOverdueJobs(today.minusDays(28));

        assertThat(result).extracting(Job::getId).doesNotContain(draftId);
    }

    @Test
    void findOverdueAssignments_returnsUnfinishedAssignmentsForOverdueJobs() {
        LocalDate today = LocalDate.now();
        Job j = job(today.minusDays(40), JobStatus.ASSIGNED);
        JobAssignment a = new JobAssignment();
        a.setJobId(j.getId());
        a.setUserId(UUID.randomUUID());
        a.setFamilyId(UUID.randomUUID());
        a.setStatus(AssignmentStatus.ASSIGNED);
        assignmentRepository.save(a);

        List<JobAssignment> result = assignmentRepository.findOverdueAssignments(today.minusDays(28));

        assertThat(result).extracting(JobAssignment::getId).contains(a.getId());
    }
}
