package com.monteweb.jobboard;

import com.monteweb.admin.AdminModuleApi;
import com.monteweb.admin.TenantConfigInfo;
import com.monteweb.family.FamilyInfo;
import com.monteweb.family.FamilyModuleApi;
import com.monteweb.jobboard.internal.model.BillingPeriod;
import com.monteweb.jobboard.internal.repository.BillingPeriodRepository;
import com.monteweb.jobboard.internal.repository.JobAssignmentRepository;
import com.monteweb.jobboard.internal.repository.JobAttachmentRepository;
import com.monteweb.jobboard.internal.repository.JobRepository;
import com.monteweb.jobboard.internal.service.JobStorageService;
import com.monteweb.jobboard.internal.service.JobboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobboardServiceSchoolYearTest {

    @Mock JobRepository jobRepository;
    @Mock JobAssignmentRepository assignmentRepository;
    @Mock BillingPeriodRepository billingPeriodRepository;
    @Mock JobAttachmentRepository attachmentRepository;
    @Mock com.monteweb.user.UserModuleApi userModuleApi;
    @Mock FamilyModuleApi familyModuleApi;
    @Mock AdminModuleApi adminModuleApi;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock JobStorageService storageService;

    JobboardService service;

    @BeforeEach
    void setUp() {
        service = new JobboardService(jobRepository, assignmentRepository, billingPeriodRepository,
                attachmentRepository, userModuleApi, familyModuleApi, adminModuleApi, eventPublisher,
                null, null, null, storageService);
    }

    private TenantConfigInfo tenantConfig() {
        return new TenantConfigInfo(
                UUID.randomUUID(), "Montessori Schule", null,
                Map.of(), Map.of(),
                new BigDecimal("10"), BigDecimal.ZERO,
                true, true, "BY", List.of(),
                null, false, true,
                true, "de", List.of("de", "en"), true,
                null, null, null, null,
                null, null, null, null,
                null, null, null,
                "DISABLED", null,
                null, null, null,
                null, null, null, null,
                "PARENT", false, false,
                null,
                null, 3310,
                null,
                null,
                false, false
        );
    }

    private BillingPeriod period(String status, LocalDate start, LocalDate end) {
        BillingPeriod p = new BillingPeriod();
        p.setId(UUID.randomUUID());
        p.setName("Schuljahr " + start.getYear() + "/" + (start.getYear() + 1));
        p.setStartDate(start);
        p.setEndDate(end);
        p.setStatus(status);
        return p;
    }

    @Test
    void listSchoolYears_mapsPeriodsAndMarksActive() {
        var active = period("ACTIVE", LocalDate.of(2025, 9, 1), LocalDate.of(2026, 8, 31));
        var closed = period("CLOSED", LocalDate.of(2024, 9, 1), LocalDate.of(2025, 8, 31));
        when(billingPeriodRepository.findAllByOrderByStartDateDesc()).thenReturn(List.of(active, closed));

        List<SchoolYearInfo> result = service.listSchoolYears();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).active()).isTrue();
        assertThat(result.get(1).active()).isFalse();
        assertThat(result.get(1).name()).isEqualTo("Schuljahr 2024/2025");
    }

    @Test
    void getFamilyHours_withoutPeriodId_usesActivePeriodDateRange() {
        UUID familyId = UUID.randomUUID();
        var active = period("ACTIVE", LocalDate.of(2025, 9, 1), LocalDate.of(2026, 8, 31));
        // FamilyInfo(id, name, avatarUrl, hoursExempt, active, soleCustody, soleCustodyApproved, members)
        var family = new FamilyInfo(familyId, "Familie Test", null, false, true, false, false, List.of());
        when(familyModuleApi.findById(familyId)).thenReturn(Optional.of(family));
        when(billingPeriodRepository.findByStatus("ACTIVE")).thenReturn(Optional.of(active));
        when(adminModuleApi.getTenantConfig()).thenReturn(tenantConfig());
        when(assignmentRepository.sumConfirmedNormalHoursByFamilyIdAndDateRange(eq(familyId), any(), any()))
                .thenReturn(new BigDecimal("6.00"));
        lenient().when(assignmentRepository.sumPendingHoursByFamilyIdAndDateRange(eq(familyId), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(assignmentRepository.sumConfirmedCleaningJobHoursByFamilyIdAndDateRange(eq(familyId), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        var hours = service.getFamilyHours(familyId, null);

        assertThat(hours).isPresent();
        assertThat(hours.get().completedHours()).isEqualByComparingTo("6.00");
    }
}
