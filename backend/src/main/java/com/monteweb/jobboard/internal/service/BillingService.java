package com.monteweb.jobboard.internal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monteweb.admin.AdminModuleApi;
import com.monteweb.cleaning.CleaningModuleApi;
import com.monteweb.family.FamilyInfo;
import com.monteweb.family.FamilyModuleApi;
import com.monteweb.jobboard.internal.dto.BillingPeriodInfo;
import com.monteweb.jobboard.internal.dto.BillingReportInfo;
import com.monteweb.jobboard.internal.dto.BillingReportInfo.BillingSummary;
import com.monteweb.jobboard.internal.dto.BillingReportInfo.FamilyBillingEntry;
import com.monteweb.jobboard.internal.dto.BillingReportInfo.FamilyMember;
import com.monteweb.jobboard.internal.dto.CreateBillingPeriodRequest;
import com.monteweb.jobboard.internal.model.BillingPeriod;
import com.monteweb.jobboard.internal.repository.BillingPeriodRepository;
import com.monteweb.jobboard.internal.repository.JobAssignmentRepository;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import com.monteweb.shared.util.PdfService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@ConditionalOnProperty(prefix = "monteweb.modules.jobboard", name = "enabled", havingValue = "true")
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final BillingPeriodRepository billingPeriodRepository;
    private final JobAssignmentRepository assignmentRepository;
    private final FamilyModuleApi familyModuleApi;
    private final AdminModuleApi adminModuleApi;
    private final CleaningModuleApi cleaningModuleApi;
    private final PdfService pdfService;
    private final ObjectMapper objectMapper;

    public BillingService(BillingPeriodRepository billingPeriodRepository,
                          JobAssignmentRepository assignmentRepository,
                          FamilyModuleApi familyModuleApi,
                          AdminModuleApi adminModuleApi,
                          @Autowired(required = false) CleaningModuleApi cleaningModuleApi,
                          PdfService pdfService,
                          ObjectMapper objectMapper) {
        this.billingPeriodRepository = billingPeriodRepository;
        this.assignmentRepository = assignmentRepository;
        this.familyModuleApi = familyModuleApi;
        this.adminModuleApi = adminModuleApi;
        this.cleaningModuleApi = cleaningModuleApi;
        this.pdfService = pdfService;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureActiveBillingPeriod() {
        if (billingPeriodRepository.findByStatus("ACTIVE").isPresent()) {
            return;
        }
        // Derive period from school vacations (Sommerferien)
        var tenantConfig = adminModuleApi.getTenantConfig();
        LocalDate[] summerVacation = findSummerVacation(tenantConfig.schoolVacations());

        LocalDate now = LocalDate.now();
        LocalDate start;
        LocalDate end;

        if (summerVacation != null) {
            // Period ends the day before summer vacation starts, next period starts on first day
            // Determine if we are before or after this year's summer vacation
            if (now.isBefore(summerVacation[0])) {
                // We're in the current school year: find previous year's summer vacation start
                end = summerVacation[0].minusDays(1);
                start = end.minusYears(1).plusDays(1);
            } else {
                // We're in summer vacation or after: new period starts at summer vacation start
                start = summerVacation[0];
                end = start.plusYears(1).minusDays(1);
            }
        } else {
            // Fallback: school year Sep-Aug
            int startYear = now.getMonthValue() >= 9 ? now.getYear() : now.getYear() - 1;
            start = LocalDate.of(startYear, 9, 1);
            end = LocalDate.of(startYear + 1, 8, 31);
        }

        var period = new BillingPeriod();
        period.setName(generateNextPeriodName(start));
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus("ACTIVE");
        billingPeriodRepository.save(period);
        log.info("Auto-created billing period: {}", period.getName());
    }

    public BillingPeriodInfo createPeriod(CreateBillingPeriodRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessException("End date must be after start date");
        }
        if (billingPeriodRepository.findByStatus("ACTIVE").isPresent()) {
            throw new BusinessException("An active billing period already exists");
        }

        var period = new BillingPeriod();
        period.setName(request.name());
        period.setStartDate(request.startDate());
        period.setEndDate(request.endDate());
        period.setStatus("ACTIVE");
        period = billingPeriodRepository.save(period);
        return toInfo(period);
    }

    @Transactional(readOnly = true)
    public List<BillingPeriodInfo> listPeriods() {
        return billingPeriodRepository.findAllByOrderByStartDateDesc().stream()
                .map(this::toInfo)
                .toList();
    }

    @Transactional(readOnly = true)
    public BillingPeriodInfo getActivePeriod() {
        return billingPeriodRepository.findByStatus("ACTIVE")
                .map(this::toInfo)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public BillingReportInfo getReport(UUID periodId) {
        var period = billingPeriodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("BillingPeriod", periodId));

        // For closed periods, return frozen report data
        if ("CLOSED".equals(period.getStatus()) && period.getReportData() != null) {
            try {
                return objectMapper.readValue(period.getReportData(), BillingReportInfo.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to read frozen report data", e);
            }
        }

        return generateReport(period);
    }

    public BillingPeriodInfo closePeriod(UUID periodId, UUID closedByUserId) {
        var period = billingPeriodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("BillingPeriod", periodId));

        if (!"ACTIVE".equals(period.getStatus())) {
            throw new BusinessException("Only active periods can be closed");
        }

        // Generate and freeze the report
        var report = generateReport(period);
        try {
            period.setReportData(objectMapper.writeValueAsString(report));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize report data", e);
        }

        period.setStatus("CLOSED");
        period.setClosedAt(Instant.now());
        period.setClosedBy(closedByUserId);
        period = billingPeriodRepository.save(period);

        // Create next period: derive dates from school vacations
        var tenantConfig = adminModuleApi.getTenantConfig();
        LocalDate[] summerVacation = findSummerVacation(tenantConfig.schoolVacations());

        LocalDate nextStart;
        LocalDate nextEnd;

        if (summerVacation != null && summerVacation[0].isAfter(period.getEndDate())) {
            // Next period starts at summer vacation, ends the day before next year's summer
            nextStart = summerVacation[0];
            nextEnd = nextStart.plusYears(1).minusDays(1);
        } else {
            // Fallback: start = end_date + 1 day, end = +1 year
            nextStart = period.getEndDate().plusDays(1);
            nextEnd = nextStart.plusYears(1).minusDays(1);
        }

        var nextPeriod = new BillingPeriod();
        nextPeriod.setName(generateNextPeriodName(nextStart));
        nextPeriod.setStartDate(nextStart);
        nextPeriod.setEndDate(nextEnd);
        nextPeriod.setStatus("ACTIVE");
        billingPeriodRepository.save(nextPeriod);

        return toInfo(period);
    }

    public byte[] exportPdf(UUID periodId) {
        var report = getReport(periodId);
        var period = report.period();
        String schoolName = adminModuleApi.getTenantConfig().schoolName();

        var sb = new StringBuilder();
        sb.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE html>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head>
                    <style>
                        body { font-family: sans-serif; font-size: 11px; margin: 1.5cm; }
                        h1 { font-size: 18px; margin-bottom: 0.2cm; }
                        h2 { font-size: 14px; color: #555; margin-bottom: 0.5cm; }
                        .summary { margin-bottom: 0.8cm; font-size: 12px; }
                        .summary span { margin-right: 1.5cm; }
                        .green { color: #16a34a; }
                        .yellow { color: #ca8a04; }
                        .red { color: #dc2626; }
                        table { width: 100%; border-collapse: collapse; font-size: 10px; }
                        th, td { border: 1px solid #ddd; padding: 4px 6px; text-align: left; }
                        th { background-color: #f5f5f5; font-weight: bold; }
                        .right { text-align: right; }
                        .members { font-size: 9px; color: #666; padding-left: 20px; }
                        .footer { margin-top: 1cm; font-size: 9px; color: #999; }
                        .negative { color: #dc2626; }
                    </style>
                </head>
                <body>
                """);
        sb.append("<h1>").append(escapeXml(schoolName)).append("</h1>\n");
        sb.append("<h2>Jahresabrechnung: ").append(escapeXml(period.name())).append("</h2>\n");
        sb.append("<p>Zeitraum: ").append(formatDate(period.startDate()))
                .append(" - ").append(formatDate(period.endDate())).append("</p>\n");

        // Summary
        var summary = report.summary();
        sb.append("<div class=\"summary\">\n");
        sb.append("<span>Familien: <b>").append(summary.totalFamilies()).append("</b></span>");
        sb.append("<span>Durchschnitt: <b>").append(summary.averageHours()).append("h</b></span>");
        sb.append("<span class=\"green\">Gr\u00fcn: <b>").append(summary.greenCount()).append("</b></span>");
        sb.append("<span class=\"yellow\">Gelb: <b>").append(summary.yellowCount()).append("</b></span>");
        sb.append("<span class=\"red\">Rot: <b>").append(summary.redCount()).append("</b></span>");
        sb.append("</div>\n");

        // Table
        sb.append("<table>\n");
        sb.append("<tr><th>Nr</th><th>Familie</th><th>Mitglieder</th>");
        sb.append("<th class=\"right\">Elternstd.</th><th class=\"right\">Putzstd.</th>");
        sb.append("<th class=\"right\">Gesamt</th><th class=\"right\">Soll</th>");
        sb.append("<th class=\"right\">Saldo</th><th class=\"right\">Soll Putz</th>");
        sb.append("<th class=\"right\">Saldo Putz</th></tr>\n");

        int nr = 1;
        for (var entry : report.families()) {
            BigDecimal balance = entry.balance() != null ? entry.balance() : BigDecimal.ZERO;
            BigDecimal cleaningBalance = entry.cleaningBalance() != null ? entry.cleaningBalance() : BigDecimal.ZERO;
            String balanceClass = balance.compareTo(BigDecimal.ZERO) < 0 ? "right negative" : "right";
            String cleanBalanceClass = cleaningBalance.compareTo(BigDecimal.ZERO) < 0 ? "right negative" : "right";
            String membersStr = entry.members().stream()
                    .map(m -> escapeXml(m.displayName()) + " (" + translateRole(m.role()) + ")")
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");

            sb.append("<tr>");
            sb.append("<td>").append(nr++).append("</td>");
            sb.append("<td>").append(escapeXml(entry.familyName())).append("</td>");
            sb.append("<td class=\"members\">").append(membersStr).append("</td>");
            sb.append("<td class=\"right\">").append(entry.jobHours() != null ? entry.jobHours() : BigDecimal.ZERO).append("h</td>");
            sb.append("<td class=\"right\">").append(entry.cleaningHours() != null ? entry.cleaningHours() : BigDecimal.ZERO).append("h</td>");
            sb.append("<td class=\"right\">").append(entry.totalHours() != null ? entry.totalHours() : BigDecimal.ZERO).append("h</td>");
            sb.append("<td class=\"right\">").append(entry.targetHours() != null ? entry.targetHours() : BigDecimal.ZERO).append("h</td>");
            sb.append("<td class=\"").append(balanceClass).append("\">")
                    .append(balance.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "")
                    .append(balance).append("h</td>");
            sb.append("<td class=\"right\">").append(entry.targetCleaningHours() != null ? entry.targetCleaningHours() : BigDecimal.ZERO).append("h</td>");
            sb.append("<td class=\"").append(cleanBalanceClass).append("\">")
                    .append(cleaningBalance.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "")
                    .append(cleaningBalance).append("h</td>");
            sb.append("</tr>\n");
        }

        sb.append("</table>\n");
        sb.append("<p class=\"footer\">Generiert am ").append(LocalDate.now()).append("</p>\n");
        sb.append("</body></html>");

        return pdfService.renderHtmlToPdf(sb.toString());
    }

    public byte[] exportCsv(UUID periodId) {
        var report = getReport(periodId);

        var csv = new StringBuilder();
        csv.append("Nr;Familie;Mitglieder;Elternstunden;Putzstunden;Gesamt;Soll;Saldo;Soll Putz;Saldo Putz;Ampel\n");

        int nr = 1;
        for (var entry : report.families()) {
            String membersStr = entry.members().stream()
                    .map(m -> m.displayName() + " (" + translateRole(m.role()) + ")")
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");

            csv.append(nr++).append(";");
            csv.append(escapeCsv(entry.familyName())).append(";");
            csv.append(escapeCsv(membersStr)).append(";");
            csv.append(entry.jobHours()).append(";");
            csv.append(entry.cleaningHours()).append(";");
            csv.append(entry.totalHours()).append(";");
            csv.append(entry.targetHours()).append(";");
            csv.append(entry.balance()).append(";");
            csv.append(entry.targetCleaningHours()).append(";");
            csv.append(entry.cleaningBalance()).append(";");
            csv.append(translateTrafficLight(entry.trafficLight())).append("\n");
        }

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] result = new byte[bom.length + bytes.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(bytes, 0, result, bom.length, bytes.length);
        return result;
    }

    // ── Report Generation ────────────────────────────────────────────────

    private BillingReportInfo generateReport(BillingPeriod period) {
        var tenantConfig = adminModuleApi.getTenantConfig();
        BigDecimal targetHours = tenantConfig.targetHoursPerFamily();
        if (targetHours == null) targetHours = BigDecimal.ZERO;
        BigDecimal targetCleaningHrs = tenantConfig.targetCleaningHours();
        if (targetCleaningHrs == null) targetCleaningHrs = BigDecimal.ZERO;

        Instant fromInstant = period.getStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant toInstant = period.getEndDate().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<FamilyInfo> allFamilies = familyModuleApi.findAll();
        List<FamilyBillingEntry> entries = new ArrayList<>();

        for (FamilyInfo family : allFamilies) {
            // Skip families exempt from hours
            if (family.hoursExempt()) continue;
            // Skip inactive families
            if (!family.active()) continue;
            // Normal job hours (all categories except Reinigung)
            BigDecimal jobHours = assignmentRepository.sumConfirmedNormalHoursByFamilyIdAndDateRange(
                    family.id(), fromInstant, toInstant);

            // Cleaning hours from Reinigung-category jobs
            BigDecimal jobCleaningHours = assignmentRepository.sumConfirmedCleaningJobHoursByFamilyIdAndDateRange(
                    family.id(), fromInstant, toInstant);

            // Legacy QR cleaning hours
            BigDecimal qrCleaningHours = BigDecimal.ZERO;
            if (cleaningModuleApi != null) {
                qrCleaningHours = cleaningModuleApi.getCleaningHoursForFamilyInRange(
                        family.id(), period.getStartDate(), period.getEndDate());
            }

            BigDecimal cleaningHours = jobCleaningHours.add(qrCleaningHours);
            BigDecimal totalHours = jobHours.add(cleaningHours);
            BigDecimal balance = totalHours.subtract(targetHours);
            BigDecimal cleaningBalance = cleaningHours.subtract(targetCleaningHrs);
            String trafficLight = calculateTrafficLight(totalHours, targetHours);

            List<FamilyMember> members = family.members().stream()
                    .map(m -> new FamilyMember(m.userId(), m.displayName(), m.role()))
                    .toList();

            entries.add(new FamilyBillingEntry(
                    family.id(), family.name(), members,
                    jobHours, cleaningHours, totalHours,
                    targetHours, balance,
                    targetCleaningHrs, cleaningBalance,
                    trafficLight));
        }

        // Sort: red first, then yellow, then green
        entries.sort((a, b) -> {
            int priority = trafficLightPriority(a.trafficLight()) - trafficLightPriority(b.trafficLight());
            if (priority != 0) return priority;
            return a.familyName().compareToIgnoreCase(b.familyName());
        });

        // Summary
        BigDecimal totalHoursAll = entries.stream()
                .map(FamilyBillingEntry::totalHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageHours = entries.isEmpty() ? BigDecimal.ZERO :
                totalHoursAll.divide(BigDecimal.valueOf(entries.size()), 1, RoundingMode.HALF_UP);
        long greenCount = entries.stream().filter(e -> "GREEN".equals(e.trafficLight())).count();
        long yellowCount = entries.stream().filter(e -> "YELLOW".equals(e.trafficLight())).count();
        long redCount = entries.stream().filter(e -> "RED".equals(e.trafficLight())).count();

        var summary = new BillingSummary(entries.size(), averageHours, totalHoursAll, greenCount, yellowCount, redCount);
        return new BillingReportInfo(toInfo(period), entries, summary);
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

    private BillingPeriodInfo toInfo(BillingPeriod period) {
        return new BillingPeriodInfo(
                period.getId(), period.getName(),
                period.getStartDate(), period.getEndDate(),
                period.getStatus(), period.getClosedAt(),
                period.getClosedBy(), period.getNotes(),
                period.getCreatedAt());
    }

    private String generateNextPeriodName(LocalDate start) {
        return "Schuljahr " + start.getYear() + "/" + (start.getYear() + 1);
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d.%02d.%d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String translateTrafficLight(String light) {
        return switch (light) {
            case "GREEN" -> "Gruen";
            case "YELLOW" -> "Gelb";
            case "RED" -> "Rot";
            default -> light;
        };
    }

    private String translateRole(String role) {
        return switch (role) {
            case "PARENT" -> "Eltern";
            case "CHILD" -> "Kind";
            default -> role;
        };
    }

    /**
     * Find the "Sommerferien" entry from school vacations config.
     * Returns [startDate, endDate] or null if not found.
     */
    private LocalDate[] findSummerVacation(List<Map<String, String>> schoolVacations) {
        if (schoolVacations == null || schoolVacations.isEmpty()) return null;
        for (Map<String, String> v : schoolVacations) {
            String name = v.get("name");
            if (name != null && name.toLowerCase().contains("sommer")) {
                try {
                    LocalDate from = LocalDate.parse(v.get("from"));
                    LocalDate to = LocalDate.parse(v.get("to"));
                    return new LocalDate[]{from, to};
                } catch (Exception e) {
                    log.warn("Failed to parse summer vacation dates: {}", v, e);
                }
            }
        }
        return null;
    }
}
