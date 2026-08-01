package com.portfolio.timetable.service;

import com.portfolio.timetable.model.ScheduleEntry;
import com.portfolio.timetable.repository.ScheduleEntryRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Checks a proposed {@link ScheduleEntry} against every existing
 * entry for the same instructor and the same room on the same day,
 * using {@link ScheduleEntry#overlaps}. Kept as its own class
 * (rather than folded into {@link ScheduleEntryService}) because the
 * conflict rule — same day, overlapping time range, same instructor
 * OR same room — is the one piece of business logic in this project
 * worth unit testing in complete isolation from Spring and the
 * database.
 */
@Service
public class ConflictDetectionService {

    private final ScheduleEntryRepository scheduleEntryRepository;

    public ConflictDetectionService(ScheduleEntryRepository scheduleEntryRepository) {
        this.scheduleEntryRepository = scheduleEntryRepository;
    }

    /**
     * Finds every existing entry that would clash with {@code candidate}.
     *
     * @param excludeEntryId when updating an existing entry, its own ID,
     *                       so it isn't reported as conflicting with itself;
     *                       {@code null} when creating a brand-new entry
     */
    public List<ScheduleEntry> findConflicts(ScheduleEntry candidate, Long excludeEntryId) {
        List<ScheduleEntry> sameInstructor = scheduleEntryRepository.findByInstructorIdAndDayOfWeek(
                candidate.getInstructor().getId(), candidate.getDayOfWeek());
        List<ScheduleEntry> sameRoom = scheduleEntryRepository.findByRoomIdAndDayOfWeek(
                candidate.getRoom().getId(), candidate.getDayOfWeek());

        // Dedupe by ID rather than relying on default Object identity via
        // Stream.distinct(), since JPA doesn't guarantee the same managed
        // instance is returned across two separate repository calls.
        Map<Long, ScheduleEntry> byId = new LinkedHashMap<>();
        for (ScheduleEntry entry : sameInstructor) {
            byId.put(entry.getId(), entry);
        }
        for (ScheduleEntry entry : sameRoom) {
            byId.put(entry.getId(), entry);
        }

        return byId.values().stream()
                .filter(existing -> excludeEntryId == null || !existing.getId().equals(excludeEntryId))
                .filter(existing -> existing.overlaps(candidate))
                .collect(Collectors.toList());
    }

    /** Convenience check for callers that only need a yes/no answer. */
    public boolean hasConflict(ScheduleEntry candidate, Long excludeEntryId) {
        return !findConflicts(candidate, excludeEntryId).isEmpty();
    }
}
