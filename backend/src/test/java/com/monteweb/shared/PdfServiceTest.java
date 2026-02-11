package com.monteweb.shared;

import com.monteweb.shared.util.PdfService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfServiceTest {

    private final PdfService pdfService = new PdfService();

    @Test
    void renderHtmlToPdf_simpleHtml_shouldReturnPdfBytes() {
        String html = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE html>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head><title>Test</title></head>
                <body><p>Hello World</p></body>
                </html>
                """;

        byte[] pdf = pdfService.renderHtmlToPdf(html);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        // PDF magic bytes: %PDF
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
        assertEquals('D', (char) pdf[2]);
        assertEquals('F', (char) pdf[3]);
    }

    @Test
    void renderHtmlToPdf_withSpecialCharacters_shouldWork() {
        String html = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE html>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head><title>Umlauts</title></head>
                <body><p>Gr&#xFC;&#xDF;e aus M&#xFC;nchen</p></body>
                </html>
                """;

        byte[] pdf = pdfService.renderHtmlToPdf(html);
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void generateHoursReport_shouldReturnValidPdf() {
        var rows = List.of(
                new PdfService.HoursReportRow("Familie Müller", "10.0", "3.0", "13.0", "2.0", "17.0"),
                new PdfService.HoursReportRow("Familie Schmidt", "5.5", "1.5", "7.0", "0.0", "23.0")
        );

        byte[] pdf = pdfService.generateHoursReport("Montessori Schule Test", rows);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        assertEquals('%', (char) pdf[0]);
    }

    @Test
    void generateHoursReport_emptyRows_shouldStillWork() {
        byte[] pdf = pdfService.generateHoursReport("Empty School", List.of());

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void generateCleaningQrCodes_shouldReturnValidPdf() {
        var entries = List.of(
                new PdfService.QrCodeEntry("2026-03-15", "14:00-16:00", "abc-123-def"),
                new PdfService.QrCodeEntry("2026-03-22", "14:00-16:00", "ghi-456-jkl")
        );

        byte[] pdf = pdfService.generateCleaningQrCodes("Grundschule Putzplan", entries);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        assertEquals('%', (char) pdf[0]);
    }

    @Test
    void generateCleaningQrCodes_emptyEntries_shouldStillWork() {
        byte[] pdf = pdfService.generateCleaningQrCodes("Empty Config", List.of());

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void generateHoursReport_withXmlSpecialChars_shouldEscape() {
        var rows = List.of(
                new PdfService.HoursReportRow("Familie <Test> & \"Sonder\"", "10.0", "3.0", "13.0", "2.0", "17.0")
        );

        byte[] pdf = pdfService.generateHoursReport("School & <Friends>", rows);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void renderHtmlToPdf_invalidHtml_shouldThrow() {
        assertThrows(RuntimeException.class, () ->
                pdfService.renderHtmlToPdf("not valid html at all"));
    }
}
