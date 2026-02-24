package com.monteweb.search.internal.service;

import com.monteweb.search.SearchResult;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@ConditionalOnProperty(prefix = "monteweb.modules.solr", name = "enabled", havingValue = "true")
public class SolrSearchService {

    private static final Logger log = LoggerFactory.getLogger(SolrSearchService.class);

    private final SolrClient solrClient;

    public SolrSearchService(SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    public List<SearchResult> search(String query, String type, int limit) {
        try {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(escapeQuery(query));
            solrQuery.setRows(limit);

            // Filter by doc_type if not ALL
            if (!"ALL".equals(type)) {
                solrQuery.addFilterQuery("doc_type:" + type);
            }

            // Highlighting
            solrQuery.setHighlight(true);
            solrQuery.setHighlightFragsize(150);
            solrQuery.setHighlightSnippets(2);
            solrQuery.addHighlightField("title");
            solrQuery.addHighlightField("content");
            solrQuery.addHighlightField("file_content");

            // Sort by score then created_at desc
            solrQuery.addSort("score", SolrQuery.ORDER.desc);
            solrQuery.addSort("created_at", SolrQuery.ORDER.desc);

            QueryResponse response = solrClient.query(solrQuery);
            Map<String, Map<String, List<String>>> highlighting = response.getHighlighting();

            List<SearchResult> results = new ArrayList<>();
            for (SolrDocument doc : response.getResults()) {
                String solrId = (String) doc.getFieldValue("id");
                String docType = (String) doc.getFieldValue("doc_type");
                String entityId = (String) doc.getFieldValue("entity_id");
                String title = getStringField(doc, "title");
                String url = getStringField(doc, "url");
                String roomName = getStringField(doc, "room_name");
                String contentType = getStringField(doc, "content_type");

                // Build subtitle
                String subtitle = buildSubtitle(docType, roomName, contentType, doc);

                // Build snippet from highlights
                String snippet = buildSnippet(highlighting, solrId);
                if (snippet == null) {
                    snippet = truncate(getStringField(doc, "content"), 150);
                }

                // Timestamp
                Date createdAt = (Date) doc.getFieldValue("created_at");
                Date updatedAt = (Date) doc.getFieldValue("updated_at");
                Instant timestamp = updatedAt != null ? updatedAt.toInstant()
                        : createdAt != null ? createdAt.toInstant() : null;

                results.add(new SearchResult(
                        entityId != null ? UUID.fromString(entityId) : null,
                        docType,
                        title,
                        subtitle,
                        snippet,
                        url,
                        timestamp
                ));
            }
            return results;
        } catch (Exception e) {
            log.error("Solr search failed for query '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    private String buildSubtitle(String docType, String roomName, String contentType, SolrDocument doc) {
        return switch (docType) {
            case "FILE" -> {
                String name = roomName != null ? roomName : "";
                String ct = contentType != null ? " (" + friendlyContentType(contentType) + ")" : "";
                Long size = (Long) doc.getFieldValue("file_size");
                String sizeStr = size != null ? " - " + formatFileSize(size) : "";
                yield name + ct + sizeStr;
            }
            case "WIKI" -> roomName != null ? "Wiki - " + roomName : "Wiki";
            case "TASK" -> roomName != null ? "Aufgabe - " + roomName : "Aufgabe";
            case "POST" -> {
                String authorName = getStringField(doc, "author_name");
                yield authorName != null ? authorName : "";
            }
            case "USER" -> getStringField(doc, "content"); // email
            case "ROOM" -> getStringField(doc, "content"); // description
            case "EVENT" -> getStringField(doc, "content"); // description
            default -> null;
        };
    }

    private String buildSnippet(Map<String, Map<String, List<String>>> highlighting, String solrId) {
        if (highlighting == null || !highlighting.containsKey(solrId)) return null;
        var docHighlights = highlighting.get(solrId);
        // Prefer file_content, then content, then title
        for (String field : List.of("file_content", "content", "title")) {
            if (docHighlights.containsKey(field) && !docHighlights.get(field).isEmpty()) {
                return String.join(" ... ", docHighlights.get(field));
            }
        }
        return null;
    }

    private String escapeQuery(String query) {
        // Escape Solr special characters but allow basic queries
        return query.replace("\\", "\\\\")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("!", "\\!")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("^", "\\^")
                .replace("~", "\\~")
                .replace(":", "\\:");
    }

    private String getStringField(SolrDocument doc, String field) {
        Object val = doc.getFieldValue(field);
        return val != null ? val.toString() : null;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    private String friendlyContentType(String contentType) {
        if (contentType == null) return "Datei";
        if (contentType.contains("pdf")) return "PDF";
        if (contentType.contains("word") || contentType.contains("docx")) return "Word";
        if (contentType.contains("excel") || contentType.contains("spreadsheet") || contentType.contains("xlsx")) return "Excel";
        if (contentType.contains("powerpoint") || contentType.contains("presentation")) return "PowerPoint";
        if (contentType.contains("image")) return "Bild";
        if (contentType.contains("video")) return "Video";
        if (contentType.contains("audio")) return "Audio";
        if (contentType.contains("text/plain")) return "Text";
        if (contentType.contains("text/csv")) return "CSV";
        if (contentType.contains("zip") || contentType.contains("archive")) return "Archiv";
        return "Datei";
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1048576) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1073741824) return String.format("%.1f MB", bytes / 1048576.0);
        return String.format("%.1f GB", bytes / 1073741824.0);
    }
}
