package com.monteweb.shared.util;

import com.monteweb.calendar.EventInfo;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Utility service for generating iCalendar (.ics) content from event data.
 * Follows RFC 5545 basics for VCALENDAR and VEVENT components.
 */
@Service
public class ICalService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    /**
     * Generates an iCalendar (.ics) file content from a list of events.
     *
     * @param events list of EventInfo objects to include
     * @return iCalendar content as a String
     */
    public String generateIcal(List<EventInfo> events) {
        var sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//MonteWeb//Calendar//DE\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");
        sb.append("METHOD:PUBLISH\r\n");

        if (events != null) {
            for (var event : events) {
                appendEvent(sb, event);
            }
        }

        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    private void appendEvent(StringBuilder sb, EventInfo event) {
        sb.append("BEGIN:VEVENT\r\n");
        sb.append("UID:").append(event.id()).append("@monteweb\r\n");

        if (event.allDay()) {
            sb.append("DTSTART;VALUE=DATE:").append(formatDate(event.startDate())).append("\r\n");
            // For all-day events, DTEND is exclusive, so add one day
            LocalDate endDate = event.endDate() != null ? event.endDate().plusDays(1) : event.startDate().plusDays(1);
            sb.append("DTEND;VALUE=DATE:").append(formatDate(endDate)).append("\r\n");
        } else {
            sb.append("DTSTART:").append(formatDateTime(event.startDate(), event.startTime())).append("\r\n");
            if (event.endDate() != null) {
                sb.append("DTEND:").append(formatDateTime(event.endDate(), event.endTime())).append("\r\n");
            }
        }

        sb.append("SUMMARY:").append(escapeIcalText(event.title())).append("\r\n");

        if (event.location() != null && !event.location().isBlank()) {
            sb.append("LOCATION:").append(escapeIcalText(event.location())).append("\r\n");
        }

        if (event.description() != null && !event.description().isBlank()) {
            sb.append("DESCRIPTION:").append(escapeIcalText(event.description())).append("\r\n");
        }

        if (event.cancelled()) {
            sb.append("STATUS:CANCELLED\r\n");
        } else {
            sb.append("STATUS:CONFIRMED\r\n");
        }

        sb.append("END:VEVENT\r\n");
    }

    private String formatDate(LocalDate date) {
        return date.format(DATE_FORMAT);
    }

    private String formatDateTime(LocalDate date, LocalTime time) {
        if (time == null) {
            return date.format(DATE_FORMAT) + "T000000";
        }
        return date.atTime(time).format(DATETIME_FORMAT);
    }

    private String escapeIcalText(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
