package com.portfolio.timetable.dto;

import java.time.Instant;
import java.util.List;

public class ErrorDtos {

    private ErrorDtos() {
    }

    /** Generic error shape for 404s, validation failures, and other 4xx/5xx responses. */
    public record ApiError(Instant timestamp, int status, String error, String message) {
        public static ApiError of(int status, String error, String message) {
            return new ApiError(Instant.now(), status, error, message);
        }
    }

    /**
     * 409 response for a scheduling conflict: the usual error fields
     * plus the specific entries the request clashed with, so the
     * caller can show the user exactly what's double-booked.
     */
    public record ScheduleConflictError(
            Instant timestamp, int status, String error, String message,
            List<ScheduleEntryDtos.Response> conflictingEntries) {
    }
}
