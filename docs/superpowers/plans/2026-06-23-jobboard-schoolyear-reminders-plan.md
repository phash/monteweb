# Jobbörse — Schuljahr-Sicht & Job-Reminder Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eltern sehen Jobboard-Stunden/erledigte Jobs standardmäßig im laufenden Schuljahr mit Umschalter auf vergangene Jahre; überfällige/unbestätigte Jobs lösen E-Mail- + In-App-Reminder aus (4 Wochen nach Termin sowie 2 Wochen vor Jahresende).

**Architecture:** Das bestehende `BillingPeriod` (admin-verwaltete „Schuljahr"-Perioden) ist die einzige Schuljahr-Quelle. Ein neuer, für Eltern lesbarer Endpoint liefert die wählbaren Jahre; `getFamilyHours`/`getAssignmentsForFamily` bekommen period-scoped Varianten (Default = aktive Periode). Ein neuer `@Scheduled` `JobReminderService` (Pattern wie `ParentLetterReminderService`) verschickt In-App-Notifications (immer) + E-Mails (wenn `monteweb.email.enabled`).

**Tech Stack:** Java 21, Spring Boot 4.0.6, Spring Modulith, Spring Data JPA, Flyway, JUnit 5 + Mockito + Testcontainers (Backend); Vue 3.5 `<script setup>`, TypeScript, Pinia (composition), PrimeVue 4, Vitest + @vue/test-utils (Frontend).

## Global Constraints

- **Modularität:** NIE aus dem `internal/`-Paket eines *anderen* Moduls importieren. Cross-Modul nur über `*ModuleApi`-Facades / Events. `MonteWebModularityTests` muss grün bleiben. (Innerhalb desselben Moduls — z.B. `JobboardController` → `JobboardService`, `BillingPeriodRepository` — ist Zugriff erlaubt.)
- **Optionale Beans:** alles im jobboard-Modul trägt `@ConditionalOnProperty(prefix = "monteweb.modules.jobboard", name = "enabled", havingValue = "true")`. Optionale Abhängigkeiten (`EmailService`) per `@Autowired(required = false)` + Null-Check.
- **`@ApplicationModuleListener`** NIE zusätzlich mit `@Transactional` annotieren. Hier nicht relevant (kein neuer Listener), aber beachten.
- **Flyway:** bestehende Migrationen NIE ändern. Neue Datei `V118__...`. Hibernate `ddl-auto: validate` → Schema und Entity müssen exakt passen.
- **Timestamps:** Entities nutzen `Instant`; Spalten `TIMESTAMP WITH TIME ZONE`. DTOs sind `record`s. UUIDs als PK.
- **Code Englisch; UI-Texte Deutsch + Englisch (i18n, beide Dateien `de.ts` + `en.ts`).** Commits: Conventional Commits.
- **Branch:** Arbeite auf `feature/jobboard-schoolyear-reminders` (bereits angelegt; enthält das Design-Doc).
- **Backend-Tests (Java 21 nicht lokal):** ausführen via Docker mit Testcontainers. Kanonischer Befehl (`BACKEND_TEST`):
  ```bash
  docker run --rm -v "$PWD/backend":/work -v ~/.m2:/root/.m2 \
    -v /var/run/docker.sock:/var/run/docker.sock -w /work \
    maven:3.9-eclipse-temurin-21 mvn -q test -Dtest=<ClassName>
  ```
  In den Schritten steht `Run (BACKEND_TEST): -Dtest=X` als Kurzform dafür.
- **Frontend-Tests:** `cd frontend && npm test` (Vitest). Einzeldatei: `npm test -- <path>`. CI nutzt `npm run test:coverage` (strenger — unbehandelte async-Fehler lassen Tests fehlschlagen; `vi.mock` vollständig halten).
- **Reminder-Empfänger „Admin":** Nutzer mit `UserRole.SUPERADMIN` (per neuer `UserModuleApi.findByRole`). Bewusst simpel gehalten; spätere Verfeinerung (Lehrer/Section-Admins) ist additiv möglich.
- **Zeitsteuerung in Reminder-Tests:** kein injizierter `Clock` — wie `ParentLetterReminderService` nutzt der Service `LocalDate.now()`/`Instant.now()`, Tests konstruieren Daten relativ zu „heute" (z.B. `scheduledDate = today.minusDays(30)`).

---

## File Structure

**Backend (neu/geändert):**
- `jobboard/SchoolYearInfo.java` — NEU, public DTO record `{id, name, startDate, endDate, active}`
- `jobboard/internal/repository/JobAssignmentRepository.java` — Range-Query für erledigte Assignments + Reminder-Queries
- `jobboard/internal/repository/JobRepository.java` — verwaiste-Jobs-Queries
- `jobboard/internal/service/JobboardService.java` — `listSchoolYears`, period-scoped `getFamilyHours`/`getAssignmentsForFamily`, DRY-Refactor von `buildFamilyHoursInfo`
- `jobboard/internal/controller/JobboardController.java` — 3 Endpoints (`/school-years`, `?periodId` auf hours+assignments)
- `jobboard/internal/model/{Job,JobAssignment,BillingPeriod}.java` — je 1 Reminder-Tracking-Feld
- `jobboard/internal/service/JobReminderService.java` — NEU, scheduled Reminder
- `notification/NotificationType.java` — Wert `JOB_OVERDUE`
- `user/UserModuleApi.java` + `user/internal/service/UserService.java` + `user/internal/repository/UserRepository.java` — `findByRole`
- `resources/db/migration/V118__job_reminders.sql` — NEU

**Frontend (neu/geändert):**
- `types/jobboard.ts` — `SchoolYearInfo` + periodId-Params
- `api/jobboard.api.ts` — `getSchoolYears`, periodId auf `getFamilyHours`/`getFamilyAssignments`
- `stores/jobboard.ts` — `schoolYears`, `selectedPeriodId`, `fetchSchoolYears`, periodId-fähige Actions
- `components/common/SchoolYearSelect.vue` — NEU
- `components/family/FamilyHoursWidget.vue` + `views/admin/AdminJobReport.vue` — Selector eingebaut
- `i18n/de.ts` + `i18n/en.ts` — `jobboard.schoolYear`, `jobboard.currentSchoolYear`

---

## Task 1: Repository — period-scoped confirmed assignments query

**Files:**
- Modify: `backend/src/main/java/com/monteweb/jobboard/internal/repository/JobAssignmentRepository.java`
- Test: `backend/src/test/java/com/monteweb/jobboard/JobAssignmentRepositoryRangeTest.java` (Create)

**Interfaces:**
- Produces: `List<JobAssignment> findConfirmedByFamilyIdAndDateRange(UUID familyId, Instant fromInstant, Instant toInstant)`

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/monteweb/jobboard/JobAssignmentRepositoryRangeTest.java`:

```java
package com.monteweb.jobboard;

import com.monteweb.config.TestContainerConfig;
import com.monteweb.jobboard.internal.model.JobAssignment;
import com.monteweb.jobboard.internal.repository.JobAssignmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestContainerConfig.class)
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
```

- [ ] **Step 2: Run test to verify it fails**

Run (BACKEND_TEST): `-Dtest=JobAssignmentRepositoryRangeTest`
Expected: compile error / FAIL — method `findConfirmedByFamilyIdAndDateRange` does not exist.

- [ ] **Step 3: Add the query**

In `JobAssignmentRepository.java`, add after the existing `findConfirmedByFamilyId` method:

```java
    @Query("""
            SELECT a FROM JobAssignment a
            WHERE a.familyId = :familyId
            AND a.status = 'COMPLETED'
            AND a.confirmed = true
            AND a.confirmedAt >= :fromInstant
            AND a.confirmedAt < :toInstant
            ORDER BY a.completedAt DESC
            """)
    List<JobAssignment> findConfirmedByFamilyIdAndDateRange(UUID familyId, Instant fromInstant, Instant toInstant);
```

(`Instant` is already imported in this file.)

- [ ] **Step 4: Run test to verify it passes**

Run (BACKEND_TEST): `-Dtest=JobAssignmentRepositoryRangeTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/monteweb/jobboard/internal/repository/JobAssignmentRepository.java \
        backend/src/test/java/com/monteweb/jobboard/JobAssignmentRepositoryRangeTest.java
git commit -m "feat(jobboard): add period-scoped confirmed-assignments query"
```

---

## Task 2: Service — SchoolYearInfo DTO + period-scoped hours/assignments

**Files:**
- Create: `backend/src/main/java/com/monteweb/jobboard/SchoolYearInfo.java`
- Modify: `backend/src/main/java/com/monteweb/jobboard/internal/service/JobboardService.java`
- Test: `backend/src/test/java/com/monteweb/jobboard/JobboardServiceSchoolYearTest.java` (Create)

**Interfaces:**
- Consumes: `BillingPeriodRepository.findById`, `BillingPeriodRepository.findByStatus("ACTIVE")`, `BillingPeriodRepository.findAllByOrderByStartDateDesc()`; `JobAssignmentRepository.findConfirmedByFamilyIdAndDateRange` (Task 1).
- Produces:
  - `record SchoolYearInfo(UUID id, String name, LocalDate startDate, LocalDate endDate, boolean active)`
  - `List<SchoolYearInfo> JobboardService.listSchoolYears()`
  - `Optional<FamilyHoursInfo> JobboardService.getFamilyHours(UUID familyId, UUID periodId)`
  - `List<JobAssignmentInfo> JobboardService.getAssignmentsForFamily(UUID familyId, UUID periodId)`

- [ ] **Step 1: Create the DTO**

Create `backend/src/main/java/com/monteweb/jobboard/SchoolYearInfo.java`:

```java
package com.monteweb.jobboard;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Public DTO: a selectable school year (backed by a billing period).
 */
public record SchoolYearInfo(
        UUID id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        boolean active
) {
}
```

- [ ] **Step 2: Write the failing test**

Create `backend/src/test/java/com/monteweb/jobboard/JobboardServiceSchoolYearTest.java`:

```java
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
    @Mock JobAttachmentRepository attachmentRepository;
    @Mock com.monteweb.user.UserModuleApi userModuleApi;
    @Mock FamilyModuleApi familyModuleApi;
    @Mock AdminModuleApi adminModuleApi;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock JobStorageService storageService;

    JobboardService service;

    @BeforeEach
    void setUp() {
        service = new JobboardService(jobRepository, assignmentRepository, attachmentRepository,
                userModuleApi, familyModuleApi, adminModuleApi, eventPublisher,
                null, null, null, storageService);
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
        var family = new FamilyInfo(familyId, "Familie Test", true, false, List.of());
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
```

NOTE before implementing: this test references `billingPeriodRepository` as a mock and `tenantConfig()`/`FamilyInfo` constructor — adjust the test to the real constructors. Inspect `com.monteweb.family.FamilyInfo` and `com.monteweb.admin.TenantConfigInfo` for their exact record components and add `@Mock BillingPeriodRepository billingPeriodRepository;` plus a `private TenantConfigInfo tenantConfig() { return new TenantConfigInfo(...); }` helper returning `targetHoursPerFamily=10`, `targetCleaningHours=0`. (The constructor signature for `JobboardService` gains `billingPeriodRepository` in Step 4 — keep the test's `new JobboardService(...)` call in sync.)

- [ ] **Step 3: Run test to verify it fails**

Run (BACKEND_TEST): `-Dtest=JobboardServiceSchoolYearTest`
Expected: compile error — `listSchoolYears`/`getFamilyHours(UUID, UUID)` and the `billingPeriodRepository` constructor arg do not exist yet.

- [ ] **Step 4: Implement in JobboardService**

4a. Add the field + constructor param. Change the field block and constructor to inject `BillingPeriodRepository` (import `com.monteweb.jobboard.internal.repository.BillingPeriodRepository` and `com.monteweb.jobboard.internal.model.BillingPeriod`, `java.time.Instant`, `java.time.ZoneId`):

```java
    private final BillingPeriodRepository billingPeriodRepository;
```

Add `BillingPeriodRepository billingPeriodRepository` as a constructor parameter (place it right after `assignmentRepository`) and assign `this.billingPeriodRepository = billingPeriodRepository;`.

4b. Refactor `buildFamilyHoursInfo` for DRY — replace the existing private method with these three methods (the all-time path keeps identical behaviour):

```java
    private FamilyHoursInfo exemptInfo(FamilyInfo family) {
        return new FamilyHoursInfo(
                family.id(), family.name(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                "GREEN", BigDecimal.ZERO, BigDecimal.ZERO, "GREEN", true);
    }

    private FamilyHoursInfo buildFamilyHoursInfo(FamilyInfo family) {
        if (family.hoursExempt()) return exemptInfo(family);
        BigDecimal confirmed = assignmentRepository.sumConfirmedNormalHoursByFamilyId(family.id());
        BigDecimal pending = assignmentRepository.sumPendingHoursByFamilyId(family.id());
        BigDecimal jobCleaning = assignmentRepository.sumConfirmedCleaningJobHoursByFamilyId(family.id());
        BigDecimal qrCleaning = cleaningModuleApi != null
                ? cleaningModuleApi.getCleaningHoursForFamily(family.id()) : BigDecimal.ZERO;
        return assembleHoursInfo(family, confirmed, pending, jobCleaning.add(qrCleaning));
    }

    private FamilyHoursInfo buildFamilyHoursInfo(FamilyInfo family, BillingPeriod period) {
        if (family.hoursExempt()) return exemptInfo(family);
        Instant from = period.getStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant to = period.getEndDate().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        BigDecimal confirmed = assignmentRepository.sumConfirmedNormalHoursByFamilyIdAndDateRange(family.id(), from, to);
        BigDecimal pending = assignmentRepository.sumPendingHoursByFamilyIdAndDateRange(family.id(), from, to);
        BigDecimal jobCleaning = assignmentRepository.sumConfirmedCleaningJobHoursByFamilyIdAndDateRange(family.id(), from, to);
        BigDecimal qrCleaning = cleaningModuleApi != null
                ? cleaningModuleApi.getCleaningHoursForFamilyInRange(family.id(), period.getStartDate(), period.getEndDate())
                : BigDecimal.ZERO;
        return assembleHoursInfo(family, confirmed, pending, jobCleaning.add(qrCleaning));
    }

    private FamilyHoursInfo assembleHoursInfo(FamilyInfo family, BigDecimal confirmed,
                                              BigDecimal pending, BigDecimal cleaningHrs) {
        var tenantConfig = adminModuleApi.getTenantConfig();
        BigDecimal targetHours = tenantConfig.targetHoursPerFamily();
        if (targetHours == null) targetHours = BigDecimal.ZERO;
        BigDecimal targetCleaningHrs = tenantConfig.targetCleaningHours();
        if (targetCleaningHrs == null) targetCleaningHrs = BigDecimal.ZERO;

        BigDecimal totalHours = confirmed.add(cleaningHrs);
        BigDecimal remaining = targetHours.subtract(totalHours).max(BigDecimal.ZERO);
        BigDecimal remainingCleaningHrs = targetCleaningHrs.subtract(cleaningHrs).max(BigDecimal.ZERO);

        return new FamilyHoursInfo(
                family.id(), family.name(), targetHours, confirmed, pending, cleaningHrs,
                totalHours, remaining, calculateTrafficLight(totalHours, targetHours),
                targetCleaningHrs, remainingCleaningHrs, calculateTrafficLight(cleaningHrs, targetCleaningHrs),
                false);
    }
```

(Keep the existing `calculateTrafficLight` method unchanged. The existing `getFamilyHours(UUID)` still calls `buildFamilyHoursInfo(family)` and is unchanged for cross-module callers.)

4c. Add the new public methods:

```java
    @Transactional(readOnly = true)
    public List<SchoolYearInfo> listSchoolYears() {
        return billingPeriodRepository.findAllByOrderByStartDateDesc().stream()
                .map(p -> new SchoolYearInfo(p.getId(), p.getName(), p.getStartDate(), p.getEndDate(),
                        "ACTIVE".equals(p.getStatus())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<FamilyHoursInfo> getFamilyHours(UUID familyId, UUID periodId) {
        return familyModuleApi.findById(familyId).map(family -> {
            BillingPeriod period = resolvePeriod(periodId);
            return period != null ? buildFamilyHoursInfo(family, period) : buildFamilyHoursInfo(family);
        });
    }

    @Transactional(readOnly = true)
    public List<JobAssignmentInfo> getAssignmentsForFamily(UUID familyId, UUID periodId) {
        BillingPeriod period = resolvePeriod(periodId);
        if (period == null) {
            return getAssignmentsForFamily(familyId);
        }
        Instant from = period.getStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant to = period.getEndDate().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return assignmentRepository.findConfirmedByFamilyIdAndDateRange(familyId, from, to).stream()
                .map(this::toAssignmentInfo)
                .toList();
    }

    private BillingPeriod resolvePeriod(UUID periodId) {
        if (periodId != null) {
            return billingPeriodRepository.findById(periodId).orElse(null);
        }
        return billingPeriodRepository.findByStatus("ACTIVE").orElse(null);
    }
```

- [ ] **Step 5: Run test to verify it passes**

Run (BACKEND_TEST): `-Dtest=JobboardServiceSchoolYearTest`
Expected: PASS. If `BillingService` (which also constructs/wires repositories) breaks compilation, no change needed — `JobboardService`'s new constructor param is added by Spring automatically.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/monteweb/jobboard/SchoolYearInfo.java \
        backend/src/main/java/com/monteweb/jobboard/internal/service/JobboardService.java \
        backend/src/test/java/com/monteweb/jobboard/JobboardServiceSchoolYearTest.java
git commit -m "feat(jobboard): period-scoped family hours/assignments + school-year list"
```

---

## Task 3: Controller — school-years + periodId endpoints

**Files:**
- Modify: `backend/src/main/java/com/monteweb/jobboard/internal/controller/JobboardController.java`
- Test: `backend/src/test/java/com/monteweb/jobboard/JobboardSchoolYearControllerTest.java` (Create)

**Interfaces:**
- Consumes: `JobboardService.listSchoolYears()`, `getFamilyHours(UUID, UUID)`, `getAssignmentsForFamily(UUID, UUID)` (Task 2).
- Produces: `GET /api/v1/jobs/school-years`; `?periodId=` query param on `GET /api/v1/jobs/family/{familyId}/hours` and `.../assignments`.

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/monteweb/jobboard/JobboardSchoolYearControllerTest.java`. Follow the existing controller-integration pattern in the repo (`@SpringBootTest @AutoConfigureMockMvc @Import(TestContainerConfig.class)`, JWT helper). Open an existing test such as `backend/src/test/java/com/monteweb/jobboard/...ControllerIntegrationTest.java` to copy the exact login/JWT helper, then assert:

```java
    @Test
    void schoolYears_returnsOkForAuthenticatedUser() throws Exception {
        String token = loginAs("eltern@monteweb.local", "test1234"); // use repo's existing helper
        mockMvc.perform(get("/api/v1/jobs/school-years")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
```

(If a school-year/billing-period seed is not present in the test DB, the array may be empty — assert it is an array, not its size.)

- [ ] **Step 2: Run test to verify it fails**

Run (BACKEND_TEST): `-Dtest=JobboardSchoolYearControllerTest`
Expected: FAIL — 404/no handler for `/api/v1/jobs/school-years`.

- [ ] **Step 3: Implement the endpoints**

In `JobboardController.java`:

3a. Add the school-years endpoint:

```java
    @GetMapping("/school-years")
    public ResponseEntity<ApiResponse<List<SchoolYearInfo>>> listSchoolYears() {
        SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(jobboardService.listSchoolYears()));
    }
```

3b. Replace the existing `getFamilyHours` handler so it accepts an optional `periodId` and defaults to the active period:

```java
    @GetMapping("/family/{familyId}/hours")
    public ResponseEntity<ApiResponse<FamilyHoursInfo>> getFamilyHours(
            @PathVariable UUID familyId,
            @RequestParam(required = false) UUID periodId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        requireFamilyMemberOrAdmin(userId, familyId);
        var hours = jobboardService.getFamilyHours(familyId, periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Family", familyId));
        return ResponseEntity.ok(ApiResponse.ok(hours));
    }
```

3c. Update the existing family-assignments handler (`GET /family/{familyId}/assignments`) to accept `periodId` and call the new overload:

```java
    @GetMapping("/family/{familyId}/assignments")
    public ResponseEntity<ApiResponse<List<JobAssignmentInfo>>> getFamilyAssignments(
            @PathVariable UUID familyId,
            @RequestParam(required = false) UUID periodId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        requireFamilyMemberOrAdmin(userId, familyId);
        return ResponseEntity.ok(ApiResponse.ok(jobboardService.getAssignmentsForFamily(familyId, periodId)));
    }
```

(`SchoolYearInfo` and `JobAssignmentInfo` resolve via the existing `import com.monteweb.jobboard.*;`. If the assignments handler had a different name/signature, replace its body to match the above.)

- [ ] **Step 4: Run test to verify it passes**

Run (BACKEND_TEST): `-Dtest=JobboardSchoolYearControllerTest`
Expected: PASS.

- [ ] **Step 5: Run the modularity test**

Run (BACKEND_TEST): `-Dtest=MonteWebModularityTests`
Expected: PASS (no new cross-module `internal` imports).

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/monteweb/jobboard/internal/controller/JobboardController.java \
        backend/src/test/java/com/monteweb/jobboard/JobboardSchoolYearControllerTest.java
git commit -m "feat(jobboard): expose school-years + period-scoped family endpoints"
```

---

## Task 4: Frontend — types + API for school years

**Files:**
- Modify: `frontend/src/types/jobboard.ts`
- Modify: `frontend/src/api/jobboard.api.ts`

**Interfaces:**
- Produces: `SchoolYearInfo`; `jobboardApi.getSchoolYears()`; optional `periodId` on `getFamilyHours`/`getFamilyAssignments`.

- [ ] **Step 1: Add the type**

In `frontend/src/types/jobboard.ts`, append:

```typescript
export interface SchoolYearInfo {
  id: string
  name: string
  startDate: string
  endDate: string
  active: boolean
}
```

- [ ] **Step 2: Extend the API module**

In `frontend/src/api/jobboard.api.ts`:

2a. Add `SchoolYearInfo` to the type import block.

2b. Add the new call and add `periodId` params to the two family calls:

```typescript
  getSchoolYears() {
    return client.get<ApiResponse<SchoolYearInfo[]>>('/jobs/school-years')
  },

  getFamilyAssignments(familyId: string, periodId?: string) {
    return client.get<ApiResponse<JobAssignmentInfo[]>>(`/jobs/family/${familyId}/assignments`, {
      params: { periodId },
    })
  },

  getFamilyHours(familyId: string, periodId?: string) {
    return client.get<ApiResponse<FamilyHoursInfo>>(`/jobs/family/${familyId}/hours`, {
      params: { periodId },
    })
  },
```

(Replace the existing `getFamilyHours` and `getFamilyAssignments` definitions. `axios` omits `undefined` params, so calling without `periodId` keeps the current behaviour and the backend defaults to the active period.)

- [ ] **Step 3: Type-check**

Run: `cd frontend && npx vue-tsc --noEmit`
Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/types/jobboard.ts frontend/src/api/jobboard.api.ts
git commit -m "feat(frontend): school-year type + API for period-scoped jobboard"
```

---

## Task 5: Frontend — jobboard store school-year state

**Files:**
- Modify: `frontend/src/stores/jobboard.ts`
- Test: `frontend/src/stores/__tests__/jobboard.schoolyear.test.ts` (Create)

**Interfaces:**
- Consumes: `jobboardApi.getSchoolYears`, `getFamilyHours(familyId, periodId)` (Task 4).
- Produces: store state `schoolYears`, `selectedPeriodId`; actions `fetchSchoolYears()`, `fetchFamilyHours(familyId, periodId?)`; both must be returned from the store.

- [ ] **Step 1: Write the failing test**

Create `frontend/src/stores/__tests__/jobboard.schoolyear.test.ts`:

```typescript
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useJobboardStore } from '@/stores/jobboard'

vi.mock('@/api/jobboard.api', () => ({
  jobboardApi: {
    getSchoolYears: vi.fn(),
    getFamilyHours: vi.fn(),
  },
}))

import { jobboardApi } from '@/api/jobboard.api'

describe('Jobboard store — school years', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchSchoolYears stores list and selects the active period by default', async () => {
    const store = useJobboardStore()
    vi.mocked(jobboardApi.getSchoolYears).mockResolvedValue({
      data: { data: [
        { id: 'p2', name: 'Schuljahr 2025/2026', startDate: '2025-09-01', endDate: '2026-08-31', active: true },
        { id: 'p1', name: 'Schuljahr 2024/2025', startDate: '2024-09-01', endDate: '2025-08-31', active: false },
      ] },
    } as any)

    await store.fetchSchoolYears()

    expect(store.schoolYears).toHaveLength(2)
    expect(store.selectedPeriodId).toBe('p2')
  })

  it('fetchFamilyHours passes the selected periodId', async () => {
    const store = useJobboardStore()
    store.selectedPeriodId = 'p1'
    vi.mocked(jobboardApi.getFamilyHours).mockResolvedValue({
      data: { data: { familyId: 'f1', familyName: 'X', completedHours: 3 } },
    } as any)

    await store.fetchFamilyHours('f1', store.selectedPeriodId)

    expect(jobboardApi.getFamilyHours).toHaveBeenCalledWith('f1', 'p1')
    expect(store.familyHours?.completedHours).toBe(3)
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npm test -- src/stores/__tests__/jobboard.schoolyear.test.ts`
Expected: FAIL — `schoolYears`/`selectedPeriodId`/`fetchSchoolYears` undefined.

- [ ] **Step 3: Implement in the store**

In `frontend/src/stores/jobboard.ts`:

3a. Add `SchoolYearInfo` to the type import.

3b. Add state refs (near the other refs):

```typescript
  const schoolYears = ref<SchoolYearInfo[]>([])
  const selectedPeriodId = ref<string | null>(null)
```

3c. Add/replace actions:

```typescript
  async function fetchSchoolYears() {
    try {
      const res = await jobboardApi.getSchoolYears()
      schoolYears.value = res.data.data
      if (!selectedPeriodId.value) {
        const active = schoolYears.value.find((y) => y.active)
        selectedPeriodId.value = active?.id ?? schoolYears.value[0]?.id ?? null
      }
    } catch (e) {
      console.error('Failed to fetch school years:', e)
    }
  }

  async function fetchFamilyHours(familyId: string, periodId?: string) {
    try {
      const res = await jobboardApi.getFamilyHours(familyId, periodId)
      familyHours.value = res.data.data
    } catch {
      familyHours.value = null
    }
  }
```

(Replace the existing single-arg `fetchFamilyHours`.)

3d. Add `schoolYears`, `selectedPeriodId`, `fetchSchoolYears` to the store's `return { ... }`. (`fetchFamilyHours` is already returned.)

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && npm test -- src/stores/__tests__/jobboard.schoolyear.test.ts`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/stores/jobboard.ts frontend/src/stores/__tests__/jobboard.schoolyear.test.ts
git commit -m "feat(frontend): jobboard store school-year state + actions"
```

---

## Task 6: Frontend — SchoolYearSelect component + i18n

**Files:**
- Create: `frontend/src/components/common/SchoolYearSelect.vue`
- Modify: `frontend/src/i18n/de.ts`, `frontend/src/i18n/en.ts`
- Test: `frontend/src/components/__tests__/SchoolYearSelect.test.ts` (Create)

**Interfaces:**
- Produces: `<SchoolYearSelect v-model="periodId" :options="schoolYears" />` — props `modelValue: string | null`, `options: SchoolYearInfo[]`; emits `update:modelValue`.

- [ ] **Step 1: Add i18n keys**

In `frontend/src/i18n/de.ts`, inside the `jobboard: { ... }` block add:

```typescript
    schoolYear: 'Schuljahr',
    currentSchoolYear: 'Aktuelles Schuljahr',
```

In `frontend/src/i18n/en.ts`, inside the `jobboard: { ... }` block add:

```typescript
    schoolYear: 'School year',
    currentSchoolYear: 'Current school year',
```

- [ ] **Step 2: Write the failing test**

Create `frontend/src/components/__tests__/SchoolYearSelect.test.ts`:

```typescript
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import SchoolYearSelect from '@/components/common/SchoolYearSelect.vue'

const i18n = createI18n({
  legacy: false,
  locale: 'de',
  messages: { de: { jobboard: { schoolYear: 'Schuljahr' } } },
})

function mountSelect(props = {}) {
  return mount(SchoolYearSelect, {
    props: { modelValue: null, options: [], ...props },
    global: {
      plugins: [i18n],
      stubs: {
        Select: {
          template: '<select class="select-stub" @change="$emit(\'update:modelValue\', $event.target.value)"><slot /></select>',
          props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'placeholder'],
          emits: ['update:modelValue'],
        },
      },
    },
  })
}

describe('SchoolYearSelect', () => {
  it('renders a select', () => {
    const wrapper = mountSelect({
      options: [{ id: 'p1', name: 'Schuljahr 2025/2026', startDate: '', endDate: '', active: true }],
    })
    expect(wrapper.find('.select-stub').exists()).toBe(true)
  })

  it('emits update:modelValue when the selection changes', async () => {
    const wrapper = mountSelect({
      modelValue: 'p1',
      options: [{ id: 'p1', name: 'A', startDate: '', endDate: '', active: true }],
    })
    await wrapper.find('.select-stub').setValue('p1')
    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
  })
})
```

- [ ] **Step 3: Run test to verify it fails**

Run: `cd frontend && npm test -- src/components/__tests__/SchoolYearSelect.test.ts`
Expected: FAIL — component file does not exist.

- [ ] **Step 4: Create the component**

Create `frontend/src/components/common/SchoolYearSelect.vue`:

```vue
<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import Select from 'primevue/select'
import type { SchoolYearInfo } from '@/types/jobboard'

const props = defineProps<{
  modelValue: string | null
  options: SchoolYearInfo[]
}>()

const emit = defineEmits<{ 'update:modelValue': [value: string | null] }>()

const { t } = useI18n()

const value = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})
</script>

<template>
  <Select
    v-model="value"
    :options="props.options"
    option-label="name"
    option-value="id"
    :placeholder="t('jobboard.schoolYear')"
    class="school-year-select"
  />
</template>

<style scoped>
.school-year-select {
  min-width: 14rem;
}
</style>
```

- [ ] **Step 5: Run test to verify it passes**

Run: `cd frontend && npm test -- src/components/__tests__/SchoolYearSelect.test.ts`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/components/common/SchoolYearSelect.vue \
        frontend/src/components/__tests__/SchoolYearSelect.test.ts \
        frontend/src/i18n/de.ts frontend/src/i18n/en.ts
git commit -m "feat(frontend): SchoolYearSelect component + i18n keys"
```

---

## Task 7: Frontend — wire selector into FamilyHoursWidget & AdminJobReport

**Files:**
- Modify: `frontend/src/components/family/FamilyHoursWidget.vue`
- Modify: `frontend/src/views/admin/AdminJobReport.vue`

**Interfaces:**
- Consumes: `SchoolYearSelect` (Task 6); store `schoolYears`/`selectedPeriodId`/`fetchSchoolYears`/`fetchFamilyHours` (Task 5).

- [ ] **Step 1: FamilyHoursWidget — load years and react to selection**

In `frontend/src/components/family/FamilyHoursWidget.vue` `<script setup>`:

1a. Import the component: `import SchoolYearSelect from '@/components/common/SchoolYearSelect.vue'`.

1b. On mount, also load years and pass the selected period; re-fetch when it changes. Replace the existing mount fetch (`jobboard.fetchFamilyHours(familyId)`) with:

```typescript
import { watch } from 'vue'
// inside onMounted (make it async):
await jobboard.fetchSchoolYears()
await jobboard.fetchFamilyHours(props.familyId, jobboard.selectedPeriodId ?? undefined)

watch(
  () => jobboard.selectedPeriodId,
  (periodId) => jobboard.fetchFamilyHours(props.familyId, periodId ?? undefined),
)
```

1c. In the full variant of the template (not the compact one — guard with the component's existing `compact`/variant flag), render the selector above the breakdown:

```vue
<SchoolYearSelect
  v-if="jobboard.schoolYears.length > 1"
  v-model="jobboard.selectedPeriodId"
  :options="jobboard.schoolYears"
/>
```

- [ ] **Step 2: AdminJobReport — note it already has billing periods**

`AdminJobReport.vue` currently calls `jobboard.fetchReport()` (all-time, no period). The period-scoped admin report already exists in `AdminBilling.vue` (`billing.fetchReport(periodId)`). To avoid duplicating period logic in the admin report, add a one-line hint linking admins to `AdminBilling` for per-year figures, and leave `AdminJobReport` showing the live all-time snapshot:

In `AdminJobReport.vue` template, near the top toolbar add:

```vue
<Message severity="info" :closable="false">
  {{ t('jobboard.reportAllTimeHint') }}
</Message>
```

Add i18n keys `jobboard.reportAllTimeHint` (de: „Diese Übersicht zeigt die Gesamtsumme. Schuljahr-Abrechnungen findest du unter Jahresabrechnung." / en: "This overview shows all-time totals. For per-school-year billing see Annual Billing.") to both `de.ts` and `en.ts`.

(Rationale: the parent-facing per-year requirement is fully covered by FamilyHoursWidget + the existing AdminBilling period report; we avoid a second period switcher in the live admin report.)

- [ ] **Step 3: Run the frontend test suite**

Run: `cd frontend && npm test`
Expected: PASS (existing FamilyHoursWidget/AdminJobReport tests still green; if a test stubbed `fetchFamilyHours` with the old single-arg signature, update the stub/assertion to the new optional arg).

- [ ] **Step 4: Type-check + commit**

Run: `cd frontend && npx vue-tsc --noEmit` → no errors.

```bash
git add frontend/src/components/family/FamilyHoursWidget.vue \
        frontend/src/views/admin/AdminJobReport.vue \
        frontend/src/i18n/de.ts frontend/src/i18n/en.ts
git commit -m "feat(frontend): school-year selector in family hours; report hint"
```

---

## Task 8: Migration + entity fields + reminder repository queries

**Files:**
- Create: `backend/src/main/resources/db/migration/V118__job_reminders.sql`
- Modify: `backend/.../jobboard/internal/model/Job.java`, `JobAssignment.java`, `BillingPeriod.java`
- Modify: `backend/.../jobboard/internal/repository/JobRepository.java`, `JobAssignmentRepository.java`
- Test: `backend/src/test/java/com/monteweb/jobboard/JobReminderQueriesTest.java` (Create)

**Interfaces:**
- Produces:
  - entity fields `Job.overdueReminderSentAt`, `JobAssignment.overdueReminderSentAt`, `BillingPeriod.yearEndReminderSentAt` (all `Instant`)
  - `List<JobAssignment> JobAssignmentRepository.findOverdueAssignments(LocalDate cutoff)`
  - `List<JobAssignment> JobAssignmentRepository.findOutstandingAssignmentsInRange(LocalDate fromDate, LocalDate toDate)`
  - `List<Job> JobRepository.findOrphanedOverdueJobs(LocalDate cutoff)`
  - `List<Job> JobRepository.findOrphanedJobsInRange(LocalDate fromDate, LocalDate toDate)`

- [ ] **Step 1: Create the migration**

Create `backend/src/main/resources/db/migration/V118__job_reminders.sql`:

```sql
-- Reminder tracking: lets the scheduled JobReminderService fire each reminder
-- exactly once. overdue_* guard the "4 weeks after the scheduled date" wave;
-- year_end_reminder_sent_at guards the once-per-period "2 weeks before period end" wave.
ALTER TABLE job_assignments ADD COLUMN overdue_reminder_sent_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE jobs ADD COLUMN overdue_reminder_sent_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE billing_periods ADD COLUMN year_end_reminder_sent_at TIMESTAMP WITH TIME ZONE;
```

- [ ] **Step 2: Add entity fields**

In `Job.java` (after `approvedAt`):

```java
    @Column(name = "overdue_reminder_sent_at")
    private Instant overdueReminderSentAt;
```

In `JobAssignment.java` (after `completedAt`):

```java
    @Column(name = "overdue_reminder_sent_at")
    private Instant overdueReminderSentAt;
```

In `BillingPeriod.java` (after `updatedAt`):

```java
    @Column(name = "year_end_reminder_sent_at")
    private Instant yearEndReminderSentAt;
```

- [ ] **Step 3: Write the failing test**

Create `backend/src/test/java/com/monteweb/jobboard/JobReminderQueriesTest.java`:

```java
package com.monteweb.jobboard;

import com.monteweb.config.TestContainerConfig;
import com.monteweb.jobboard.internal.model.Job;
import com.monteweb.jobboard.internal.model.JobAssignment;
import com.monteweb.jobboard.internal.repository.JobAssignmentRepository;
import com.monteweb.jobboard.internal.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestContainerConfig.class)
class JobReminderQueriesTest {

    @Autowired JobRepository jobRepository;
    @Autowired JobAssignmentRepository assignmentRepository;

    private Job job(LocalDate scheduledDate, JobStatus status) {
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
```

- [ ] **Step 4: Run test to verify it fails**

Run (BACKEND_TEST): `-Dtest=JobReminderQueriesTest`
Expected: compile error — query methods do not exist. (If `ddl-auto: validate` already fails because the migration ran but entity lacks the field, that confirms Step 2 is needed.)

- [ ] **Step 5: Add the queries**

In `JobAssignmentRepository.java` add (imports `java.time.LocalDate` — add it):

```java
    @Query("""
            SELECT a FROM JobAssignment a, Job j
            WHERE a.jobId = j.id
            AND j.scheduledDate IS NOT NULL
            AND j.scheduledDate <= :cutoff
            AND a.overdueReminderSentAt IS NULL
            AND (a.status IN ('ASSIGNED', 'IN_PROGRESS')
                 OR (a.status = 'COMPLETED' AND a.confirmed = false))
            """)
    List<JobAssignment> findOverdueAssignments(LocalDate cutoff);

    @Query("""
            SELECT a FROM JobAssignment a, Job j
            WHERE a.jobId = j.id
            AND j.scheduledDate IS NOT NULL
            AND j.scheduledDate >= :fromDate AND j.scheduledDate <= :toDate
            AND (a.status IN ('ASSIGNED', 'IN_PROGRESS')
                 OR (a.status = 'COMPLETED' AND a.confirmed = false))
            """)
    List<JobAssignment> findOutstandingAssignmentsInRange(LocalDate fromDate, LocalDate toDate);
```

In `JobRepository.java` add (ensure `java.time.LocalDate` and `java.util.List` imports exist; `Job` is the entity in this package):

```java
    @Query("""
            SELECT j FROM Job j
            WHERE j.scheduledDate IS NOT NULL
            AND j.scheduledDate <= :cutoff
            AND j.status NOT IN ('COMPLETED', 'CANCELLED')
            AND j.overdueReminderSentAt IS NULL
            AND NOT EXISTS (SELECT a FROM JobAssignment a WHERE a.jobId = j.id AND a.status <> 'CANCELLED')
            """)
    List<Job> findOrphanedOverdueJobs(LocalDate cutoff);

    @Query("""
            SELECT j FROM Job j
            WHERE j.scheduledDate IS NOT NULL
            AND j.scheduledDate >= :fromDate AND j.scheduledDate <= :toDate
            AND j.status NOT IN ('COMPLETED', 'CANCELLED')
            AND NOT EXISTS (SELECT a FROM JobAssignment a WHERE a.jobId = j.id AND a.status <> 'CANCELLED')
            """)
    List<Job> findOrphanedJobsInRange(LocalDate fromDate, LocalDate toDate);
```

(`JobRepository` references the `JobAssignment` entity in the subquery — both live in `com.monteweb.jobboard.internal.model`, no import needed in JPQL.)

- [ ] **Step 6: Run test to verify it passes**

Run (BACKEND_TEST): `-Dtest=JobReminderQueriesTest`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/resources/db/migration/V118__job_reminders.sql \
        backend/src/main/java/com/monteweb/jobboard/internal/model/Job.java \
        backend/src/main/java/com/monteweb/jobboard/internal/model/JobAssignment.java \
        backend/src/main/java/com/monteweb/jobboard/internal/model/BillingPeriod.java \
        backend/src/main/java/com/monteweb/jobboard/internal/repository/JobRepository.java \
        backend/src/main/java/com/monteweb/jobboard/internal/repository/JobAssignmentRepository.java \
        backend/src/test/java/com/monteweb/jobboard/JobReminderQueriesTest.java
git commit -m "feat(jobboard): V118 reminder-tracking columns + overdue/orphan queries"
```

---

## Task 9: NotificationType.JOB_OVERDUE + UserModuleApi.findByRole

**Files:**
- Modify: `backend/.../notification/NotificationType.java`
- Modify: `backend/.../user/UserModuleApi.java`, `user/internal/service/UserService.java`, `user/internal/repository/UserRepository.java`
- Test: `backend/src/test/java/com/monteweb/user/UserServiceFindByRoleTest.java` (Create)

**Interfaces:**
- Produces: `NotificationType.JOB_OVERDUE`; `List<UserInfo> UserModuleApi.findByRole(UserRole role)` (active users only).

- [ ] **Step 1: Add the enum value**

In `NotificationType.java`, add `JOB_OVERDUE,` to the enum (e.g. after `JOB_COMPLETED,`).

- [ ] **Step 2: Add the repository finder**

In `UserRepository.java` add:

```java
    List<User> findByRoleAndActiveTrue(UserRole role);
```

(`List`, `User`, `UserRole` are already imported in this file.)

- [ ] **Step 3: Add to the facade interface**

In `UserModuleApi.java` add (alongside `findByIds`):

```java
    /** Finds all active users with the given base role. */
    List<UserInfo> findByRole(UserRole role);
```

- [ ] **Step 4: Write the failing test**

Create `backend/src/test/java/com/monteweb/user/UserServiceFindByRoleTest.java`. Follow the repo's existing `UserService`/`UserServiceTest` setup (open an existing user test to copy how `UserService` is constructed or `@Autowired`). Assert:

```java
    @Test
    void findByRole_returnsActiveSuperadmins() {
        List<UserInfo> admins = userService.findByRole(UserRole.SUPERADMIN);
        assertThat(admins).isNotNull();
        assertThat(admins).allSatisfy(u -> assertThat(u.role()).isEqualTo(UserRole.SUPERADMIN));
    }
```

(If user tests are `@SpringBootTest @Import(TestContainerConfig.class)`, the seed `admin@monteweb.local` (SUPERADMIN) gives ≥1 result — additionally assert `isNotEmpty()`.)

- [ ] **Step 5: Run test to verify it fails**

Run (BACKEND_TEST): `-Dtest=UserServiceFindByRoleTest`
Expected: FAIL — `findByRole` not defined.

- [ ] **Step 6: Implement in UserService**

In `UserService.java` add (mirrors `findByIds` at line ~365, uses the existing `toUserInfo` mapper):

```java
    @Override
    public List<UserInfo> findByRole(UserRole role) {
        return userRepository.findByRoleAndActiveTrue(role).stream()
                .map(this::toUserInfo)
                .toList();
    }
```

- [ ] **Step 7: Run test to verify it passes**

Run (BACKEND_TEST): `-Dtest=UserServiceFindByRoleTest`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/monteweb/notification/NotificationType.java \
        backend/src/main/java/com/monteweb/user/UserModuleApi.java \
        backend/src/main/java/com/monteweb/user/internal/service/UserService.java \
        backend/src/main/java/com/monteweb/user/internal/repository/UserRepository.java \
        backend/src/test/java/com/monteweb/user/UserServiceFindByRoleTest.java
git commit -m "feat: JOB_OVERDUE notification type + UserModuleApi.findByRole"
```

---

## Task 10: JobReminderService (scheduled reminders)

**Files:**
- Create: `backend/src/main/java/com/monteweb/jobboard/internal/service/JobReminderService.java`
- Test: `backend/src/test/java/com/monteweb/jobboard/JobReminderServiceTest.java` (Create)

**Interfaces:**
- Consumes: `JobRepository.findOrphanedOverdueJobs/findOrphanedJobsInRange`, `JobAssignmentRepository.findOverdueAssignments/findOutstandingAssignmentsInRange` (Task 8); `BillingPeriodRepository.findByStatus`; `UserModuleApi.findById/findByRole` (Task 9); `NotificationModuleApi.sendNotification`; optional `EmailService.sendGenericEmail`; `NotificationType.JOB_OVERDUE`.

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/monteweb/jobboard/JobReminderServiceTest.java`:

```java
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
```

NOTE: confirm the exact `UserInfo` record component order (Task references show: `id, email, firstName, lastName, displayName, phone, avatarUrl, role, specialRoles, assignedRoles, active, darkMode`) and adjust `user(...)` if it differs.

- [ ] **Step 2: Run test to verify it fails**

Run (BACKEND_TEST): `-Dtest=JobReminderServiceTest`
Expected: compile error — `JobReminderService` does not exist.

- [ ] **Step 3: Create the service**

Create `backend/src/main/java/com/monteweb/jobboard/internal/service/JobReminderService.java`:

```java
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
```

- [ ] **Step 4: Run test to verify it passes**

Run (BACKEND_TEST): `-Dtest=JobReminderServiceTest`
Expected: PASS.

- [ ] **Step 5: Run modularity + a broad sanity build**

Run (BACKEND_TEST): `-Dtest=MonteWebModularityTests`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/monteweb/jobboard/internal/service/JobReminderService.java \
        backend/src/test/java/com/monteweb/jobboard/JobReminderServiceTest.java
git commit -m "feat(jobboard): scheduled overdue + year-end job reminders (in-app + email)"
```

---

## Final verification (after all tasks)

- [ ] **Backend full suite** — Run (BACKEND_TEST): drop `-Dtest=...` to run everything; expect green (watch the shared-Postgres gotcha: no test mutates seed data without `@AfterEach` cleanup).
- [ ] **Frontend** — `cd frontend && npm run test:coverage` (CI-strict) and `npx vue-tsc --noEmit`; expect green.
- [ ] **Build images** — `docker compose build backend frontend` to confirm the migration validates against a fresh schema (`ddl-auto: validate`).
- [ ] **Manual smoke** (optional, app up): log in as `eltern@monteweb.local`, open the family hours widget → it shows the active school year and the `SchoolYearSelect` switches periods; `GET /api/v1/jobs/school-years` returns the periods.
- [ ] **PR** — open against `main`; note the CI split (E2E/Docker Build run only on `main`; Trivy gate can red the Docker Build job).

## Self-Review (author)

- **Spec coverage:** Schuljahr-Sicht Eltern → Tasks 2,3,5,6,7. Default = aktive Periode → Task 2 (`resolvePeriod` null→ACTIVE) + Task 3 (controller). Abrechnung pro Schuljahr → bereits via BillingPeriod; Eltern-Stunden Task 2. Reminder Welle 1 (4 Wochen) → Tasks 8,10. Reminder Welle 2 (2 Wochen vor Jahresende) → Tasks 8,10. Verwaiste Jobs → Ersteller+Admins → Task 10 `remindForOrphanedJob`. E-Mail+In-App, E-Mail optional → Task 10 `notify`. Migration/Tracking → Task 8.
- **Placeholders:** none for new code. Three explicit "inspect existing X then match" notes (FamilyInfo/TenantConfigInfo constructors in Task 2 test; controller login helper in Task 3 test; UserInfo component order in Task 10 test) — these are real-signature confirmations, with the exact known shapes provided; not vague TODOs.
- **Type consistency:** `getFamilyHours(UUID, UUID)`, `getAssignmentsForFamily(UUID, UUID)`, `listSchoolYears()`, `findByRole(UserRole)`, `findOverdueAssignments(LocalDate)`, `findOrphanedOverdueJobs(LocalDate)`, `JOB_OVERDUE`, `SchoolYearInfo` used identically across tasks. Frontend `getSchoolYears`/`getFamilyHours(familyId, periodId)`/store `selectedPeriodId` consistent across Tasks 4–7.
