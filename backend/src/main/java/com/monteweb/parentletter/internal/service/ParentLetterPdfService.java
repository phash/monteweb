package com.monteweb.parentletter.internal.service;

import com.monteweb.parentletter.ParentLetterDetailInfo;
import com.monteweb.parentletter.RecipientStatus;
import com.monteweb.shared.util.PdfService;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@ConditionalOnProperty(prefix = "monteweb.modules", name = "parentletter.enabled", havingValue = "true")
public class ParentLetterPdfService {

    private final PdfService pdfService;
    private final Parser markdownParser = Parser.builder().build();
    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.of("Europe/Berlin"));

    public ParentLetterPdfService(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    public byte[] generateLetterPdf(ParentLetterDetailInfo letter, String resolvedContent) {
        var sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" ");
        sb.append("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>");
        sb.append("<style>");
        sb.append("body{font-family:sans-serif;font-size:12pt;margin:2cm;line-height:1.6}");
        sb.append(".meta{margin-bottom:1cm;color:#666;font-size:10pt}");
        sb.append("h1{font-size:16pt}h2{font-size:14pt}h3{font-size:12pt}");
        sb.append("table{width:100%;border-collapse:collapse}");
        sb.append("th,td{border:1px solid #ccc;padding:4pt 8pt;text-align:left}");
        sb.append("th{background:#f5f5f5}");
        sb.append("</style></head><body>");

        sb.append("<div class=\"meta\">");
        sb.append("<div>").append(escapeXml(letter.roomName())).append("</div>");
        sb.append("<div>").append(escapeXml(letter.creatorName())).append("</div>");
        if (letter.sendDate() != null) {
            sb.append("<div>Datum: ").append(DATE_FMT.format(letter.sendDate())).append("</div>");
        }
        sb.append("</div>");

        sb.append("<h1>").append(escapeXml(letter.title())).append("</h1>");

        Node doc = markdownParser.parse(resolvedContent);
        sb.append(htmlRenderer.render(doc));

        sb.append("</body></html>");
        return pdfService.renderHtmlToPdf(sb.toString());
    }

    public byte[] generateTrackingPdf(ParentLetterDetailInfo letter) {
        var sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" ");
        sb.append("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>");
        sb.append("<style>");
        sb.append("body{font-family:sans-serif;font-size:10pt;margin:2cm}");
        sb.append("h1{font-size:14pt}");
        sb.append("table{width:100%;border-collapse:collapse;margin-top:1cm}");
        sb.append("th,td{border:1px solid #ccc;padding:4pt 8pt;text-align:left}");
        sb.append("th{background:#f0f0f0;font-weight:bold}");
        sb.append(".summary{margin-top:1cm;padding:8pt;background:#f9f9f9;border:1px solid #ddd}");
        sb.append(".confirmed{color:#22c55e}.open{color:#ef4444}.read{color:#3b82f6}");
        sb.append("</style></head><body>");

        sb.append("<h1>R&#252;cklauf: ").append(escapeXml(letter.title())).append("</h1>");
        sb.append("<div>Raum: ").append(escapeXml(letter.roomName())).append("</div>");
        sb.append("<div>Erstellt von: ").append(escapeXml(letter.creatorName())).append("</div>");
        if (letter.deadline() != null) {
            sb.append("<div>Frist: ").append(DATE_FMT.format(letter.deadline())).append("</div>");
        }

        sb.append("<table><thead><tr>");
        sb.append("<th>Sch&#252;ler/in</th><th>Elternteil</th><th>Familie</th>");
        sb.append("<th>Status</th><th>Best&#228;tigt am</th>");
        sb.append("</tr></thead><tbody>");

        for (var r : letter.recipients()) {
            sb.append("<tr>");
            sb.append("<td>").append(escapeXml(r.studentName())).append("</td>");
            sb.append("<td>").append(escapeXml(r.parentName())).append("</td>");
            sb.append("<td>").append(escapeXml(r.familyName())).append("</td>");
            String statusClass = switch (r.status()) {
                case CONFIRMED -> "confirmed";
                case READ -> "read";
                case OPEN -> "open";
            };
            String statusLabel = switch (r.status()) {
                case CONFIRMED -> "Best&#228;tigt";
                case READ -> "Gelesen";
                case OPEN -> "Offen";
            };
            sb.append("<td class=\"").append(statusClass).append("\">").append(statusLabel).append("</td>");
            sb.append("<td>").append(r.confirmedAt() != null ? DATE_FMT.format(r.confirmedAt()) : "-").append("</td>");
            sb.append("</tr>");
        }

        sb.append("</tbody></table>");

        long confirmed = letter.recipients().stream()
                .filter(r -> r.status() == RecipientStatus.CONFIRMED).count();
        int total = letter.totalRecipients();
        double rate = total > 0 ? (confirmed * 100.0 / total) : 0;

        sb.append("<div class=\"summary\">");
        sb.append("<strong>").append(confirmed).append(" von ").append(total).append(" best&#228;tigt</strong>");
        sb.append(" (R&#252;cklaufquote: ").append(String.format("%.0f", rate)).append("%)");
        sb.append("</div>");

        sb.append("</body></html>");
        return pdfService.renderHtmlToPdf(sb.toString());
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
