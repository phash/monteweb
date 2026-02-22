package com.monteweb.shared.util;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfService.class);

    /**
     * Renders an HTML string to PDF bytes.
     *
     * @param html well-formed XHTML content
     * @return PDF as byte array
     */
    public byte[] renderHtmlToPdf(String html) {
        try (var os = new ByteArrayOutputStream()) {
            var builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, "/");
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF: {}", e.getMessage(), e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    /**
     * Generates an hours report PDF from structured data.
     */
    public byte[] generateHoursReport(String schoolName, java.util.List<HoursReportRow> rows) {
        var sb = new StringBuilder();
        sb.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE html>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head>
                    <style>
                        body { font-family: sans-serif; font-size: 12px; margin: 2cm; }
                        h1 { font-size: 18px; margin-bottom: 0.5cm; }
                        h2 { font-size: 14px; color: #555; margin-bottom: 1cm; }
                        table { width: 100%; border-collapse: collapse; }
                        th, td { border: 1px solid #ddd; padding: 6px 8px; text-align: left; }
                        th { background-color: #f5f5f5; font-weight: bold; }
                        .right { text-align: right; }
                        .footer { margin-top: 1cm; font-size: 10px; color: #999; }
                    </style>
                </head>
                <body>
                """);
        sb.append("<h1>").append(escapeXml(schoolName)).append("</h1>\n");
        sb.append("<h2>Familien-Stundenbericht</h2>\n");
        sb.append("<table>\n");
        sb.append("<tr><th>Familie</th><th class=\"right\">Elternstunden</th><th class=\"right\">Putzstunden</th><th class=\"right\">Gesamt</th><th class=\"right\">Ausstehend</th><th class=\"right\">Verbleibend</th></tr>\n");

        for (var row : rows) {
            sb.append("<tr>");
            sb.append("<td>").append(escapeXml(row.familyName())).append("</td>");
            sb.append("<td class=\"right\">").append(row.jobHours()).append("</td>");
            sb.append("<td class=\"right\">").append(row.cleaningHours()).append("</td>");
            sb.append("<td class=\"right\">").append(row.totalHours()).append("</td>");
            sb.append("<td class=\"right\">").append(row.pendingHours()).append("</td>");
            sb.append("<td class=\"right\">").append(row.remainingHours()).append("</td>");
            sb.append("</tr>\n");
        }

        sb.append("</table>\n");
        sb.append("<p class=\"footer\">Generiert am ").append(java.time.LocalDate.now()).append("</p>\n");
        sb.append("</body></html>");

        return renderHtmlToPdf(sb.toString());
    }

    /**
     * Generates a QR code page PDF for cleaning slots.
     */
    public byte[] generateCleaningQrCodes(String configTitle, java.util.List<QrCodeEntry> entries) {
        var sb = new StringBuilder();
        sb.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE html>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head>
                    <style>
                        body { font-family: sans-serif; font-size: 12px; margin: 2cm; }
                        h1 { font-size: 18px; margin-bottom: 1cm; }
                        .slot { margin-bottom: 1cm; padding: 0.5cm; border: 1px solid #ddd; page-break-inside: avoid; }
                        .slot-title { font-weight: bold; font-size: 14px; }
                        .slot-date { color: #555; margin-top: 0.2cm; }
                        .qr-token { font-family: monospace; font-size: 16px; margin-top: 0.3cm; padding: 0.3cm; background: #f5f5f5; }
                    </style>
                </head>
                <body>
                """);
        sb.append("<h1>QR-Codes: ").append(escapeXml(configTitle)).append("</h1>\n");

        for (var entry : entries) {
            sb.append("<div class=\"slot\">\n");
            sb.append("<div class=\"slot-title\">").append(escapeXml(entry.slotDate())).append("</div>\n");
            sb.append("<div class=\"slot-date\">").append(escapeXml(entry.timeRange())).append("</div>\n");
            sb.append("<div class=\"qr-token\">Token: ").append(escapeXml(entry.qrToken())).append("</div>\n");
            sb.append("</div>\n");
        }

        sb.append("</body></html>");

        return renderHtmlToPdf(sb.toString());
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    public record HoursReportRow(
            String familyName,
            String jobHours,
            String cleaningHours,
            String totalHours,
            String pendingHours,
            String remainingHours
    ) {}

    public record QrCodeEntry(
            String slotDate,
            String timeRange,
            String qrToken
    ) {}
}
