package com.portfolio.timetable.service;

import com.portfolio.timetable.model.*;
import com.portfolio.timetable.repository.ScheduleEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConflictDetectionServiceTest {

    @Mock
    private ScheduleEntryRepository scheduleEntryRepository;

    private ConflictDetectionService conflictDetectionService;

    private Course course;
    private Instructor instructorA;
    private Instructor instructorB;
    private Room roomA;
    private Room roomB;

    @BeforeEach
    void setUp() {
        conflictDetectionService = new ConflictDetectionService(scheduleEntryRepository);
        course = new Course("CS101", "Intro to Programming", 3, null);
        instructorA = withId(new Instructor("Dr. Rao", "rao@example.com", null), 1L);
        instructorB = withId(new Instructor("Dr. Iyer", "iyer@example.com", null), 2L);
        roomA = withId(new Room("101", "Main Building", 40), 10L);
        roomB = withId(new Room("102", "Main Building", 30), 20L);
    }

    /** Test-only helper: entity IDs are normally assigned by the database, so we set them via reflection for these unit tests. */
    private <T> T withId(T entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private ScheduleEntry entry(Long id, Instructor instructor, Room room, DayOfWeek day, String start, String end) {
        ScheduleEntry entry = new ScheduleEntry(course, instructor, room, day, LocalTime.parse(start), LocalTime.parse(end));
        return withId(entry, id);
    }

    @Test
    void reportsConflictWhenInstructorAlreadyBookedAtOverlappingTime() {
        ScheduleEntry existing = entry(1L, instructorA, roomB, DayOfWeek.MONDAY, "09:00", "10:00");
        when(scheduleEntryRepository.findByInstructorIdAndDayOfWeek(1L, DayOfWeek.MONDAY))
                .thenReturn(List.of(existing));
        when(scheduleEntryRepository.findByRoomIdAndDayOfWeek(any(), any()))
                .thenReturn(List.of());

        ScheduleEntry candidate = new ScheduleEntry(course, instructorA, roomA, DayOfWeek.MONDAY,
                LocalTime.parse("09:30"), LocalTime.parse("10:30"));

        List<ScheduleEntry> conflicts = conflictDetectionService.findConflicts(candidate, null);
        assertEquals(1, conflicts.size());
        assertEquals(existing, conflicts.get(0));
    }

    @Test
    void reportsConflictWhenRoomAlreadyBookedAtOverlappingTime() {
        ScheduleEntry existing = entry(1L, instructorB, roomA, DayOfWeek.MONDAY, "09:00", "10:00");
        when(scheduleEntryRepository.findByInstructorIdAndDayOfWeek(any(), any()))
                .thenReturn(List.of());
        when(scheduleEntryRepository.findByRoomIdAndDayOfWeek(10L, DayOfWeek.MONDAY))
                .thenReturn(List.of(existing));

        ScheduleEntry candidate = new ScheduleEntry(course, instructorA, roomA, DayOfWeek.MONDAY,
                LocalTime.parse("09:30"), LocalTime.parse("10:30"));

        List<ScheduleEntry> conflicts = conflictDetectionService.findConflicts(candidate, null);
        assertEquals(1, conflicts.size());
    }

    @Test
    void noConflictWhenTimesDoNotOverlap() {
        ScheduleEntry existing = entry(1L, instructorA, roomA, DayOfWeek.MONDAY, "09:00", "10:00");
        when(scheduleEntryRepository.findByInstructorIdAndDayOfWeek(1L, DayOfWeek.MONDAY))
                .thenReturn(List.of(existing));
        when(scheduleEntryRepository.findByRoomIdAndDayOfWeek(10L, DayOfWeek.MONDAY))
                .thenReturn(List.of(existing));

        ScheduleEntry candidate = new ScheduleEntry(course, instructorA, roomA, DayOfWeek.MONDAY,
                LocalTime.parse("14:00"), LocalTime.parse("15:00"));

        assertTrue(conflictDetectionService.findConflicts(candidate, null).isEmpty());
        assertFalse(conflictDetectionService.hasConflict(candidate, null));
    }

    @Test
    void excludesGivenEntryIdFromConflictsWhenUpdating() {
        // The entry being updated would otherwise "conflict with itself"
        ScheduleEntry existing = entry(5L, instructorA, roomA, DayOfWeek.MONDAY, "09:00", "10:00");
        when(scheduleEntryRepository.findByInstructorIdAndDayOfWeek(1L, DayOfWeek.MONDAY))
                .thenReturn(List.of(existing));
        when(scheduleEntryRepository.findByRoomIdAndDayOfWeek(10L, DayOfWeek.MONDAY))
                .thenReturn(List.of(existing));

        ScheduleEntry candidate = new ScheduleEntry(course, instructorA, roomA, DayOfWeek.MONDAY,
                LocalTime.parse("09:00"), LocalTime.parse("10:00"));

        assertTrue(conflictDetectionService.findConflicts(candidate, 5L).isEmpty());
    }

    @Test
    void reportsASharedConflictOnlyOnceWhenBothInstructorAndRoomMatch() {
        // Same existing entry returned by both lookups (matches both the
        // candidate's instructor AND its room) must be reported only once.
        ScheduleEntry existing = entry(1L, instructorA, roomA, DayOfWeek.MONDAY, "09:00", "10:00");
        when(scheduleEntryRepository.findByInstructorIdAndDayOfWeek(1L, DayOfWeek.MONDAY))
                .thenReturn(List.of(existing));
        when(scheduleEntryRepository.findByRoomIdAndDayOfWeek(10L, DayOfWeek.MONDAY))
                .thenReturn(List.of(existing));

        ScheduleEntry candidate = new ScheduleEntry(course, instructorA, roomA, DayOfWeek.MONDAY,
                LocalTime.parse("09:30"), LocalTime.parse("10:30"));

        List<ScheduleEntry> conflicts = conflictDetectionService.findConflicts(candidate, null);
        assertEquals(1, conflicts.size());
    }
}
