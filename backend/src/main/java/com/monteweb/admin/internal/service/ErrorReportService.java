package com.monteweb.admin.internal.service;

import com.monteweb.admin.internal.dto.ErrorReportInfo;
import com.monteweb.admin.internal.dto.SubmitErrorReportRequest;
import com.monteweb.admin.internal.model.ErrorReport;
import com.monteweb.admin.internal.repository.ErrorReportRepository;
import com.monteweb.admin.internal.repository.TenantConfigRepository;
import com.monteweb.shared.exception.BadRequestException;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import com.monteweb.shared.exception.UnhandledExceptionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ErrorReportService {

    private static final Logger log = LoggerFactory.getLogger(ErrorReportService.class);
    private static final Set<String> VALID_STATUSES = Set.of("NEW", "REPORTED", "RESOLVED", "IGNORED");

    private final ErrorReportRepository errorReportRepository;
    private final TenantConfigRepository tenantConfigRepository;
    private final RestTemplate restTemplate;

    public ErrorReportService(ErrorReportRepository errorReportRepository,
                              TenantConfigRepository tenantConfigRepository) {
        this.errorReportRepository = errorReportRepository;
        this.tenantConfigRepository = tenantConfigRepository;
        this.restTemplate = new RestTemplate();
    }

    @Transactional
    public void submitReport(SubmitErrorReportRequest request, UUID userId) {
        String fingerprint = calculateFingerprint(request.source(), request.errorType(),
                request.message(), request.location());

        Optional<ErrorReport> existing = errorReportRepository.findByFingerprint(fingerprint);
        if (existing.isPresent()) {
            var report = existing.get();
            report.setOccurrenceCount(report.getOccurrenceCount() + 1);
            report.setLastSeenAt(Instant.now());
            if (request.stackTrace() != null && (report.getStackTrace() == null
                    || request.stackTrace().length() > report.getStackTrace().length())) {
                report.setStackTrace(request.stackTrace());
            }
            errorReportRepository.save(report);
        } else {
            var report = new ErrorReport();
            report.setFingerprint(fingerprint);
            report.setSource(request.source());
            report.setErrorType(request.errorType());
            report.setMessage(request.message());
            report.setStackTrace(request.stackTrace());
            report.setLocation(request.location());
            report.setUserId(userId);
            report.setUserAgent(request.userAgent());
            report.setRequestUrl(sanitizeUrl(request.requestUrl()));
            errorReportRepository.save(report);
        }
    }

    @EventListener
    @Transactional
    public void handleUnhandledException(UnhandledExceptionEvent event) {
        try {
            var request = new SubmitErrorReportRequest(
                    "BACKEND",
                    event.type(),
                    event.message() != null ? event.message() : "Unknown error",
                    event.stackTrace(),
                    event.location(),
                    null,
                    null
            );
            submitReport(request, null);
        } catch (Exception ex) {
            log.warn("Failed to record unhandled exception event", ex);
        }
    }

    public Page<ErrorReportInfo> findAll(String status, String source, Pageable pageable) {
        return errorReportRepository.findFiltered(status, source, pageable)
                .map(this::toInfo);
    }

    public ErrorReportInfo findById(UUID id) {
        return toInfo(errorReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Error report not found")));
    }

    @Transactional
    public ErrorReportInfo updateStatus(UUID id, String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new BadRequestException("Invalid status: " + status + ". Must be one of: " + VALID_STATUSES);
        }
        var report = errorReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Error report not found"));
        report.setStatus(status);
        return toInfo(errorReportRepository.save(report));
    }

    @Transactional
    public void deleteReport(UUID id) {
        if (!errorReportRepository.existsById(id)) {
            throw new ResourceNotFoundException("Error report not found");
        }
        errorReportRepository.deleteById(id);
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public ErrorReportInfo createGithubIssue(UUID errorId) {
        var report = errorReportRepository.findById(errorId)
                .orElseThrow(() -> new ResourceNotFoundException("Error report not found"));

        var config = tenantConfigRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new BusinessException("Tenant configuration not found"));

        if (config.getGithubRepo() == null || config.getGithubRepo().isBlank()) {
            throw new BusinessException("GitHub repository not configured");
        }
        if (config.getGithubPat() == null || config.getGithubPat().isBlank()) {
            throw new BusinessException("GitHub PAT not configured");
        }

        String truncatedMessage = report.getMessage().length() > 80
                ? report.getMessage().substring(0, 80) + "..."
                : report.getMessage();
        String title = "[ErrorReport] " + escapeMarkdownTitle(
                (report.getErrorType() != null ? report.getErrorType() + ": " : "") + truncatedMessage);

        String body = "## Error Report\n\n"
                + "| Field | Value |\n"
                + "|-------|-------|\n"
                + "| **Source** | " + escapeMarkdownCell(report.getSource()) + " |\n"
                + "| **Type** | " + escapeMarkdownCell(report.getErrorType() != null ? report.getErrorType() : "N/A") + " |\n"
                + "| **Location** | " + escapeMarkdownCell(report.getLocation() != null ? report.getLocation() : "N/A") + " |\n"
                + "| **Occurrences** | " + report.getOccurrenceCount() + " |\n"
                + "| **First Seen** | " + report.getFirstSeenAt() + " |\n"
                + "| **Last Seen** | " + report.getLastSeenAt() + " |\n"
                + "| **Fingerprint** | `" + escapeMarkdownCell(report.getFingerprint()) + "` |\n\n"
                + "### Message\n\n"
                + "```\n" + escapeCodeBlock(report.getMessage()) + "\n```\n\n";

        if (report.getStackTrace() != null) {
            String stackTrace = report.getStackTrace().length() > 5000
                    ? report.getStackTrace().substring(0, 5000) + "\n... (truncated)"
                    : report.getStackTrace();
            body += "### Stack Trace\n\n```\n" + escapeCodeBlock(stackTrace) + "\n```\n";
        }

        String url = "https://api.github.com/repos/" + config.getGithubRepo() + "/issues";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + config.getGithubPat());
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("title", title);
        requestBody.put("body", body);
        requestBody.put("labels", List.of("bug", "error-report"));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String issueUrl = (String) response.getBody().get("html_url");
                report.setGithubIssueUrl(issueUrl);
                report.setStatus("REPORTED");
                return toInfo(errorReportRepository.save(report));
            } else {
                throw new BusinessException("GitHub API returned non-success status: " + response.getStatusCode());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create GitHub issue", e);
            throw new BusinessException("Failed to create GitHub issue: " + e.getMessage());
        }
    }

    @Transactional
    public void updateGithubConfig(String repo, String pat) {
        var config = tenantConfigRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Tenant configuration not found"));
        config.setGithubRepo(repo);
        config.setGithubPat(pat);
        tenantConfigRepository.save(config);
    }

    private ErrorReportInfo toInfo(ErrorReport entity) {
        return new ErrorReportInfo(
                entity.getId(),
                entity.getFingerprint(),
                entity.getSource(),
                entity.getErrorType(),
                entity.getMessage(),
                entity.getStackTrace(),
                entity.getLocation(),
                entity.getUserId(),
                entity.getUserAgent(),
                entity.getRequestUrl(),
                entity.getOccurrenceCount(),
                entity.getFirstSeenAt(),
                entity.getLastSeenAt(),
                entity.getStatus(),
                entity.getGithubIssueUrl(),
                entity.getCreatedAt()
        );
    }

    String calculateFingerprint(String source, String errorType, String message, String location) {
        String normalizedMessage = normalizeMessage(message);
        String input = (source != null ? source : "")
                + (errorType != null ? errorType : "")
                + normalizedMessage
                + (location != null ? location : "");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String normalizeMessage(String message) {
        if (message == null) return "";
        String normalized = message.length() > 200 ? message.substring(0, 200) : message;
        // Replace UUIDs with placeholder
        normalized = normalized.replaceAll(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
                "<UUID>");
        // Replace numbers with placeholder
        normalized = normalized.replaceAll("\\d+", "<NUM>");
        return normalized;
    }

    /**
     * DSGVO Fix (Art. 5 Abs. 1 lit. c – Datenminimierung): Entfernt Query-Parameter aus URLs,
     * da diese sensible Daten (z. B. Token, personenbezogene IDs) enthalten können.
     * Nur der URL-Pfad wird gespeichert.
     */
    static String sanitizeUrl(String url) {
        if (url == null) return null;
        int queryStart = url.indexOf('?');
        return queryStart >= 0 ? url.substring(0, queryStart) : url;
    }

    /**
     * Escape user-controlled text for safe use in GitHub markdown table cells.
     * Prevents @mention pings and pipe characters from breaking table layout.
     */
    private static String escapeMarkdownCell(String text) {
        if (text == null) return "";
        return text.replace("|", "\\|").replace("@", "&#64;");
    }

    /**
     * Escape user-controlled text for safe use in GitHub issue titles.
     * Prevents @mention pings.
     */
    private static String escapeMarkdownTitle(String text) {
        if (text == null) return "";
        return text.replace("@", "&#64;");
    }

    /**
     * Escape user-controlled text for safe inclusion in fenced code blocks.
     * Prevents premature code block termination via triple backticks.
     */
    private static String escapeCodeBlock(String text) {
        if (text == null) return "";
        return text.replace("```", "` ` `");
    }
}
