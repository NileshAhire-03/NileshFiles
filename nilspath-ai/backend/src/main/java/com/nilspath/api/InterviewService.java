package com.nilspath.api;

import java.time.*;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InterviewService {
    private final Applications applications;
    private final Interviews interviews;
    InterviewService(Applications applications, Interviews interviews) {
        this.applications = applications;
        this.interviews = interviews;
    }

    @Transactional
    public Interview schedule(ApplicationRecord application, String scheduledAt, String notes) {
        OffsetDateTime date;
        try { date = OffsetDateTime.parse(scheduledAt); }
        catch (DateTimeParseException | NullPointerException ex) {
            throw new IllegalArgumentException("Interview date must include a valid date, time and UTC offset.");
        }
        if (!date.toInstant().isAfter(Instant.now()))
            throw new IllegalArgumentException("Interview date must be in the future.");
        // Both changes commit together; a failed insert rolls back the status.
        Interview interview = interviews.saveAndFlush(new Interview(application.id, date.toString(), notes));
        application.status = "INTERVIEW";
        applications.saveAndFlush(application);
        return interview;
    }
}
