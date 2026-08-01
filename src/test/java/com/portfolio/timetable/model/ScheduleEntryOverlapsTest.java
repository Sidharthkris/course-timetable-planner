package com.portfolio.timetable.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests of {@link ScheduleEntry#overlaps}, with no Spring
 * context and no database — just plain object construction, so this
 * is the fastest and least ambiguous test of the conflict rule itself.
 */
class ScheduleEntryOverlapsTest {

    private final Course course = new Course("CS101", "Intro to Programming", 3, null);
    private final Instructor instructor = new Instructor("Dr. Rao", "rao@example.com", null);
    private final Room room = new Room("101", "Main Building", 40);

    private ScheduleEntry entry(DayOfWeek day, String start, String end) {
        return new ScheduleEntry(course, instructor, room, day, LocalTime.parse(start), LocalTime.parse(end));
    }

    @Test
    void identicalTimeRangesOverlap() {
        ScheduleEntry a = entry(DayOfWeek.MONDAY, "09:00", "10:00");
        ScheduleEntry b = entry(DayOfWeek.MONDAY, "09:00", "10:00");
        assertTrue(a.overlaps(b));
    }

    @Test
    void partiallyOverlappingRangesOverlap() {
        ScheduleEntry a = entry(DayOfWeek.MONDAY, "09:00", "10:00");
        ScheduleEntry b = entry(DayOfWeek.MONDAY, "09:30", "10:30");
        assertTrue(a.overlaps(b));
        assertTrue(b.overlaps(a)); // symmetry
    }

    @Test
    void oneRangeFullyInsideAnotherOverlaps() {
        ScheduleEntry a = entry(DayOfWeek.MONDAY, "09:00", "12:00");
        ScheduleEntry b = entry(DayOfWeek.MONDAY, "10:00", "11:00");
        assertTrue(a.overlaps(b));
    }

    @Test
    void backToBackRangesDoNotOverlap() {
        ScheduleEntry a = entry(DayOfWeek.MONDAY, "09:00", "10:00");
        ScheduleEntry b = entry(DayOfWeek.MONDAY, "10:00", "11:00");
        assertFalse(a.overlaps(b));
    }

    @Test
    void nonOverlappingRangesOnSameDayDoNotOverlap() {
        ScheduleEntry a = entry(DayOfWeek.MONDAY, "09:00", "10:00");
        ScheduleEntry b = entry(DayOfWeek.MONDAY, "14:00", "15:00");
        assertFalse(a.overlaps(b));
    }

    @Test
    void identicalTimeRangesOnDifferentDaysDoNotOverlap() {
        ScheduleEntry a = entry(DayOfWeek.MONDAY, "09:00", "10:00");
        ScheduleEntry b = entry(DayOfWeek.TUESDAY, "09:00", "10:00");
        assertFalse(a.overlaps(b));
    }
}
