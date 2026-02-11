package com.monteweb.jobboard.internal.controller;

import com.monteweb.admin.AdminModuleApi;
import com.monteweb.jobboard.FamilyHoursInfo;
import com.monteweb.jobboard.internal.service.JobboardService;
import com.monteweb.shared.util.PdfService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs/report")
@ConditionalOnProperty(prefix = "monteweb.modules.jobboard", name = "enabled", havingValue = "true")
public class JobboardExportController {

    private final JobboardService jobboardService;
    private final PdfService pdfService;
    private final AdminModuleApi adminModuleApi;

    public JobboardExportController(JobboardService jobboardService,
                                    PdfService pdfService,
                                    AdminModuleApi adminModuleApi) {
        this.jobboardService = jobboardService;
        this.pdfService = pdfService;
        this.adminModuleApi = adminModuleApi;
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv() {
        List<FamilyHoursInfo> report = jobboardService.getAllFamilyHoursReport();

        StringBuilder csv = new StringBuilder();
        csv.append("Familie;Zielstunden;Geleistete Stunden;Ausstehend;Verbleibend;Ampel\n");

        for (var entry : report) {
            csv.append(escapeCsv(entry.familyName())).append(";");
            csv.append(entry.targetHours()).append(";");
            csv.append(entry.completedHours()).append(";");
            csv.append(entry.pendingHours()).append(";");
            csv.append(entry.remainingHours()).append(";");
            csv.append(translateTrafficLight(entry.trafficLight())).append("\n");
        }

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        // Add BOM for Excel
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] result = new byte[bom.length + bytes.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(bytes, 0, result, bom.length, bytes.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"elternstunden-report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(result);
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf() {
        List<FamilyHoursInfo> report = jobboardService.getAllFamilyHoursReport();
        String schoolName = adminModuleApi.getTenantConfig().schoolName();

        var rows = report.stream()
                .map(e -> new PdfService.HoursReportRow(
                        e.familyName(),
                        e.completedHours().toString(),
                        e.completedHours().toString(),
                        e.pendingHours().toString(),
                        e.remainingHours().toString()
                ))
                .toList();

        byte[] pdf = pdfService.generateHoursReport(schoolName, rows);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"elternstunden-report.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
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
}
