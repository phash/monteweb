package com.monteweb.calendar.internal.service;

import com.monteweb.calendar.internal.model.ICalEvent;
import com.monteweb.calendar.internal.model.ICalSubscription;
import com.monteweb.calendar.internal.repository.ICalEventRepository;
import com.monteweb.calendar.internal.repository.ICalSubscriptionRepository;
import com.monteweb.shared.util.SsrfProtectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
@ConditionalOnProperty(prefix = "monteweb.modules", name = "calendar.enabled", havingValue = "true")
public class ICalImportService {

    private static final Logger log = LoggerFactory.getLogger(ICalImportService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter ICAL_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private final ICalSubscriptionRepository subscriptionRepository;
    private final ICalEventRepository eventRepository;
    private final HttpClient httpClient;

    public ICalImportService(ICalSubscriptionRepository subscriptionRepository,
                             ICalEventRepository eventRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.eventRepository = eventRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                // NEVER: redirects are followed manually so each hop is re-validated against
                // the SSRF allow-list. NORMAL would follow a 30x to an internal host
                // (cloud metadata, minio/postgres/redis, ...) without re-validation.
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public List<ICalSubscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
    }

    public ICalSubscription createSubscription(String name, String url, String color, UUID createdBy) {
        // Validate URL before persisting to fail fast on private/internal targets
        SsrfProtectionUtils.validateUrl(url);

        var sub = new ICalSubscription();
        sub.setName(name);
        sub.setUrl(url);
        sub.setColor(color != null ? color : "#6366f1");
        sub.setCreatedBy(createdBy);
        sub = subscriptionRepository.save(sub);
        syncSubscription(sub.getId());
        return sub;
    }

    public void deleteSubscription(UUID id) {
        eventRepository.deleteBySubscriptionId(id);
        subscriptionRepository.deleteById(id);
    }

    private static final int MAX_RESPONSE_BYTES = 1024 * 1024; // 1 MB
    private static final int MAX_REDIRECTS = 3;

    public void syncSubscription(UUID subId) {
        var subOpt = subscriptionRepository.findById(subId);
        if (subOpt.isEmpty()) return;

        var sub = subOpt.get();
        try {
            // SSRF protection: follow redirects manually and re-validate EVERY hop
            // (initial URL + each redirect target) so a public URL cannot 30x-redirect
            // to a private/internal host (cloud metadata, minio/postgres/redis, ...).
            HttpResponse<java.io.InputStream> response = null;
            String currentUrl = sub.getUrl();
            for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
                SsrfProtectionUtils.validateUrl(currentUrl);
                var uri = URI.create(currentUrl);
                var request = HttpRequest.newBuilder()
                        .uri(uri)
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                var resp = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                int status = resp.statusCode();
                if (status >= 300 && status < 400) {
                    var location = resp.headers().firstValue("Location").orElse(null);
                    if (location == null || location.isBlank()) {
                        log.warn("iCal fetch from {} returned a redirect with no Location header", currentUrl);
                        return;
                    }
                    currentUrl = uri.resolve(location).toString();
                    continue;
                }
                response = resp;
                break;
            }
            if (response == null) {
                log.warn("Too many redirects fetching iCal from {}", sub.getUrl());
                return;
            }
            if (response.statusCode() != 200) {
                log.warn("Failed to fetch iCal from {}: HTTP {}", sub.getUrl(), response.statusCode());
                return;
            }

            String body = SsrfProtectionUtils.readLimited(response.body(), MAX_RESPONSE_BYTES);
            List<ParsedEvent> parsed = parseICalEvents(body);

            for (var pe : parsed) {
                var existing = eventRepository.findBySubscriptionIdAndUid(sub.getId(), pe.uid);
                ICalEvent event;
                if (existing.isPresent()) {
                    event = existing.get();
                } else {
                    event = new ICalEvent();
                    event.setSubscriptionId(sub.getId());
                    event.setUid(pe.uid);
                }
                event.setTitle(pe.title);
                event.setDescription(pe.description);
                event.setLocation(pe.location);
                event.setStartDate(pe.startDate);
                event.setEndDate(pe.endDate);
                event.setStartTime(pe.startTime);
                event.setEndTime(pe.endTime);
                event.setAllDay(pe.allDay);
                eventRepository.save(event);
            }

            sub.setLastSyncedAt(Instant.now());
            subscriptionRepository.save(sub);
            log.info("Synced iCal subscription '{}': {} events", sub.getName(), parsed.size());
        } catch (Exception e) {
            log.error("Error syncing iCal subscription '{}': {}", sub.getName(), e.getMessage());
        }
    }

    public List<ICalEvent> getImportedEvents(LocalDate from, LocalDate to) {
        return eventRepository.findByStartDateBetween(from, to);
    }

    @Scheduled(fixedDelay = 3600000)
    public void syncAllActiveSubscriptions() {
        var active = subscriptionRepository.findByActiveTrue();
        for (var sub : active) {
            syncSubscription(sub.getId());
        }
    }

    // --- iCal parsing ---

    private List<ParsedEvent> parseICalEvents(String icalContent) {
        List<ParsedEvent> events = new ArrayList<>();
        String[] blocks = icalContent.split("BEGIN:VEVENT");

        for (int i = 1; i < blocks.length; i++) {
            String block = blocks[i];
            int endIdx = block.indexOf("END:VEVENT");
            if (endIdx < 0) continue;
            block = block.substring(0, endIdx);

            // Unfold lines (RFC 5545: lines starting with space/tab are continuations)
            block = block.replace("\r\n ", "").replace("\r\n\t", "");

            String uid = extractProperty(block, "UID");
            String summary = extractProperty(block, "SUMMARY");
            if (uid == null || summary == null) continue;

            var pe = new ParsedEvent();
            pe.uid = uid;
            pe.title = unescapeIcalText(summary);
            pe.description = unescapeIcalText(extractProperty(block, "DESCRIPTION"));
            pe.location = unescapeIcalText(extractProperty(block, "LOCATION"));

            parseDateTimes(block, pe);
            events.add(pe);
        }

        return events;
    }

    private void parseDateTimes(String block, ParsedEvent pe) {
        String dtStart = extractProperty(block, "DTSTART");
        String dtEnd = extractProperty(block, "DTEND");

        if (dtStart == null) return;

        if (dtStart.length() == 8) {
            // DATE only format: 20261225
            pe.allDay = true;
            pe.startDate = LocalDate.parse(dtStart, DATE_FORMAT);
            if (dtEnd != null && dtEnd.length() >= 8) {
                pe.endDate = LocalDate.parse(dtEnd.substring(0, 8), DATE_FORMAT);
            } else {
                pe.endDate = pe.startDate;
            }
        } else {
            // DATETIME format: 20261225T180000 (local), 20261225T180000Z (UTC),
            // or with a TZID parameter (DTSTART;TZID=Europe/Berlin:20261225T180000).
            pe.allDay = false;

            ZoneId targetZone = ZoneId.systemDefault();

            LocalDateTime start = toLocalDateTime(dtStart, extractTzid(block, "DTSTART"), targetZone);
            pe.startDate = start.toLocalDate();
            pe.startTime = formatTime(start);

            if (dtEnd != null && dtEnd.length() >= 13) {
                LocalDateTime end = toLocalDateTime(dtEnd, extractTzid(block, "DTEND"), targetZone);
                pe.endDate = end.toLocalDate();
                pe.endTime = formatTime(end);
            } else {
                pe.endDate = pe.startDate;
            }
        }
    }

    /**
     * Converts an iCal date-time value into a {@link LocalDateTime} expressed in {@code targetZone}.
     *  - Values ending in {@code Z} are UTC and are converted to the target zone.
     *  - When a {@code TZID} is supplied, the value is interpreted in that zone and converted.
     *  - Otherwise the value is treated as floating local time and returned unchanged.
     */
    private LocalDateTime toLocalDateTime(String value, String tzid, ZoneId targetZone) {
        boolean utc = value.endsWith("Z");
        String raw = utc ? value.substring(0, value.length() - 1) : value;
        LocalDateTime local = LocalDateTime.parse(raw, ICAL_DATETIME_FORMAT);

        if (utc) {
            return local.atZone(ZoneOffset.UTC).withZoneSameInstant(targetZone).toLocalDateTime();
        }
        if (tzid != null) {
            try {
                return local.atZone(ZoneId.of(tzid)).withZoneSameInstant(targetZone).toLocalDateTime();
            } catch (Exception e) {
                // Unknown TZID -> fall back to treating the value as floating local time
                log.debug("Unknown iCal TZID '{}', treating time as floating local", tzid);
                return local;
            }
        }
        return local;
    }

    private String formatTime(LocalDateTime dt) {
        return String.format("%02d:%02d", dt.getHour(), dt.getMinute());
    }

    /**
     * Extracts the {@code TZID} parameter for a given property from the raw VEVENT block,
     * e.g. {@code DTSTART;TZID=Europe/Berlin:...} -> {@code Europe/Berlin}. Returns null if absent.
     */
    private String extractTzid(String block, String name) {
        for (String line : block.split("\n")) {
            line = line.trim();
            if (line.startsWith(name + ";")) {
                int colonIdx = line.indexOf(':');
                String params = colonIdx >= 0 ? line.substring(name.length() + 1, colonIdx)
                        : line.substring(name.length() + 1);
                for (String param : params.split(";")) {
                    if (param.startsWith("TZID=")) {
                        return param.substring("TZID=".length()).trim();
                    }
                }
            }
        }
        return null;
    }

    private String extractProperty(String block, String name) {
        for (String line : block.split("\n")) {
            line = line.trim();
            if (line.startsWith(name + ":")) {
                return line.substring(name.length() + 1).trim();
            }
            // Handle parameters like DESCRIPTION;LANGUAGE=en:Some text
            if (line.startsWith(name + ";")) {
                int colonIdx = line.indexOf(':');
                if (colonIdx >= 0) {
                    return line.substring(colonIdx + 1).trim();
                }
            }
        }
        return null;
    }

    private String unescapeIcalText(String text) {
        if (text == null) return null;
        return text
                .replace("\\n", "\n")
                .replace("\\,", ",")
                .replace("\\;", ";")
                .replace("\\\\", "\\");
    }

    private static class ParsedEvent {
        String uid;
        String title;
        String description;
        String location;
        LocalDate startDate;
        LocalDate endDate;
        String startTime;
        String endTime;
        boolean allDay = true;
    }
}
