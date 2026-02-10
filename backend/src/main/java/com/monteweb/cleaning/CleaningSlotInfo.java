package com.monteweb.cleaning;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CleaningSlotInfo(
        UUID id,
        UUID configId,
        UUID sectionId,
        String sectionName,
        String configTitle,
        LocalDate slotDate,
        LocalTime startTime,
        LocalTime endTime,
        int minParticipants,
        int maxParticipants,
        int currentRegistrations,
        CleaningSlotStatus status,
        boolean cancelled,
        List<RegistrationInfo> registrations
) {
    public record RegistrationInfo(
            UUID id,
            UUID userId,
            String userName,
            UUID familyId,
            boolean checkedIn,
            boolean checkedOut,
            Integer actualMinutes,
            boolean noShow,
            boolean swapOffered
    ) {
    }
}
