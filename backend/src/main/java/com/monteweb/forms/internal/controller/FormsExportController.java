package com.monteweb.forms.internal.controller;

import com.monteweb.forms.internal.service.FormsService;
import com.monteweb.shared.util.PdfService;
import com.monteweb.shared.util.SecurityUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/forms")
@ConditionalOnProperty(prefix = "monteweb.modules", name = "forms.enabled", havingValue = "true")
public class FormsExportController {

    private final FormsService formsService;
    private final PdfService pdfService;

    public FormsExportController(FormsService formsService, PdfService pdfService) {
        this.formsService = formsService;
        this.pdfService = pdfService;
    }

    @GetMapping(value = "/{id}/results/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        String csv = formsService.generateCsv(id, userId);

        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        // Add BOM for Excel
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] result = new byte[bom.length + bytes.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(bytes, 0, result, bom.length, bytes.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"form-results.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(result);
    }

    @GetMapping(value = "/{id}/results/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        String html = formsService.generateResultsHtml(id, userId);
        byte[] pdf = pdfService.renderHtmlToPdf(html);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"form-results.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
