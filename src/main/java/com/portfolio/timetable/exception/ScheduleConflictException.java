package com.portfolio.timetable.exception;

import com.portfolio.timetable.model.ScheduleEntry;

import java.util.List;

/**
 * Thrown when a proposed {@link ScheduleEntry} would double-book an
 * instructor or a room. Carries the specific entries it conflicts
 * with so the API response can tell the caller exactly what clashed
 * and why, not just that "something" did.
 */
public class ScheduleConflictException extends RuntimeException {

    private final List<ScheduleEntry> conflictingEntries;

    public ScheduleConflictException(String message, List<ScheduleEntry> conflictingEntries) {
        super(message);
        this.conflictingEntries = conflictingEntries;
    }

    public List<ScheduleEntry> getConflictingEntries() {
        return conflictingEntries;
    }
}
