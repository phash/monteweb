package com.monteweb.jobboard;

import com.monteweb.TestContainerConfig;
import com.monteweb.jobboard.internal.model.JobAssignment;
import com.monteweb.jobboard.internal.repository.JobAssignmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestContainerConfig.class)
@Sql(statements = "SET session_replication_role = 'replica'",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class JobAssignmentRepositoryRangeTest {

    @Autowired
    JobAssignmentRepository repository;

    private JobAssignment assignment(UUID familyId, Instant confirmedAt, BigDecimal hours) {
        JobAssignment a = new JobAssignment();
        a.setJobId(UUID.randomUUID());
        a.setUserId(UUID.randomUUID());
        a.setFamilyId(familyId);
        a.setStatus(AssignmentStatus.COMPLETED);
        a.setConfirmed(true);
        a.setActualHours(hours);
        a.setConfirmedAt(confirmedAt);
        a.setCompletedAt(confirmedAt);
        return a;
    }

    @Test
    void findConfirmedByFamilyIdAndDateRange_returnsOnlyAssignmentsConfirmedInRange() {
        UUID familyId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant inRange = now.minus(5, ChronoUnit.DAYS);
        Instant outOfRange = now.minus(60, ChronoUnit.DAYS);
        repository.save(assignment(familyId, inRange, new BigDecimal("3.00")));
        repository.save(assignment(familyId, outOfRange, new BigDecimal("9.00")));

        Instant from = now.minus(30, ChronoUnit.DAYS);
        Instant to = now.plus(1, ChronoUnit.DAYS);

        List<JobAssignment> result = repository.findConfirmedByFamilyIdAndDateRange(familyId, from, to);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActualHours()).isEqualByComparingTo("3.00");
    }
}
