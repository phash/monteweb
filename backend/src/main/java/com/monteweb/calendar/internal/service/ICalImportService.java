package com.monteweb.calendar.internal.service;

import com.monteweb.calendar.internal.model.ICalEvent;
import com.monteweb.calendar.internal.model.ICalSubscription;
import com.monteweb.calendar.internal.repository.ICalEventRepository;
import com.monteweb.calendar.internal.repository.ICalSubscriptionRepository;
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
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
@ConditionalOnProperty(prefix = "monteweb.modules", name = "calendar.enabled", havingValue = "true")
public class ICalImportService {

    private static final Logger log = LoggerFactory.getLogger(ICalImportService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ICalSubscriptionRepository subscriptionRepository;
    private final ICalEventRepository eventRepository;
    private final HttpClient httpClient;

    public ICalImportService(ICalSubscriptionRepository subscriptionRepository,
                             ICalEventRepository eventRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.eventRepository = eventRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public List<ICalSubscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
    }

    public ICalSubscription createSubscription(String name, String url, String color, UUID createdBy) {
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

    public void syncSubscription(UUID subId) {
        var subOpt = subscriptionRepository.findById(subId);
        if (subOpt.isEmpty()) return;

        var sub = subOpt.get();
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(sub.getUrl()))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Failed to fetch iCal from {}: HTTP {}", sub.getUrl(), response.statusCode());
                return;
            }

            String body = response.body();
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
            // DATETIME format: 20261225T180000 or 20261225T180000Z
            pe.allDay = false;
            pe.startDate = LocalDate.parse(dtStart.substring(0, 8), DATE_FORMAT);
            pe.startTime = dtStart.substring(9, 11) + ":" + dtStart.substring(11, 13);

            if (dtEnd != null && dtEnd.length() >= 13) {
                pe.endDate = LocalDate.parse(dtEnd.substring(0, 8), DATE_FORMAT);
                pe.endTime = dtEnd.substring(9, 11) + ":" + dtEnd.substring(11, 13);
            } else {
                pe.endDate = pe.startDate;
            }
        }
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
