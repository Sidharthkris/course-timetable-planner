package com.portfolio.timetable.web;

import com.portfolio.timetable.dto.CourseDtos;
import com.portfolio.timetable.dto.DepartmentDtos;
import com.portfolio.timetable.dto.InstructorDtos;
import com.portfolio.timetable.dto.RoomDtos;
import com.portfolio.timetable.dto.ScheduleEntryDtos.Response;
import com.portfolio.timetable.web.CalendarGridBuilder.CalendarGrid;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link CalendarGridBuilder} has zero Spring/JPA dependency, so this
 * is a plain unit test — no {@code @SpringBootTest}, no database, no
 * security context. Verified standalone with {@code javac}/{@code java}
 * before being added here.
 */
class CalendarGridBuilderTest {

    private Response entry(long id, DayOfWeek day, String start, String end,
                            String courseCode, String instructorName, String roomNumber) {
        DepartmentDtos.Response dept = new DepartmentDtos.Response(1L, "CS", "Computer Science");
        CourseDtos.Response course = new CourseDtos.Response(id, courseCode, courseCode + " title", 3, dept);
        InstructorDtos.Response instructor = new InstructorDtos.Response(id, instructorName, instructorName + "@x.com", dept);
        RoomDtos.Response room = new RoomDtos.Response(id, roomNumber, "Main", 40);
        return new Response(id, course, instructor, room, day, LocalTime.parse(start), LocalTime.parse(end));
    }

    @Test
    void placesEntryInTheCorrectDayAndHourlyRow() {
        Response e1 = entry(1, DayOfWeek.MONDAY, "09:00", "11:00", "CS101", "Dr. Rao", "101");
        CalendarGrid grid = CalendarGridBuilder.build(List.of(e1));

        int row = CalendarGridBuilder.rowIndexFor(LocalTime.of(9, 0));
        List<Response> cell = grid.cellsByDay().get(DayOfWeek.MONDAY).get(row);

        assertEquals(1, cell.size());
        assertEquals(e1, cell.get(0));
    }

    @Test
    void nonHourAlignedStartTimeFallsIntoItsContainingHourlyRow() {
        Response e = entry(1, DayOfWeek.WEDNESDAY, "14:30", "15:30", "CS102", "Dr. Iyer", "102");
        CalendarGrid grid = CalendarGridBuilder.build(List.of(e));

        int row = CalendarGridBuilder.rowIndexFor(LocalTime.of(14, 30));
        assertEquals(6, row); // 14:00 is the 7th row (index 6) counting from 08:00
        assertEquals(1, grid.cellsByDay().get(DayOfWeek.WEDNESDAY).get(row).size());
    }

    @Test
    void concurrentEntriesInDifferentRoomsBothAppearInTheSameCell() {
        // Same day, same start time, different room/instructor — a legitimate
        // real-world case the conflict detector explicitly allows.
        Response e1 = entry(1, DayOfWeek.MONDAY, "09:00", "10:00", "CS101", "Dr. Rao", "101");
        Response e2 = entry(2, DayOfWeek.MONDAY, "09:00", "10:00", "DS101", "Dr. Iyer", "202");

        CalendarGrid grid = CalendarGridBuilder.build(List.of(e1, e2));
        int row = CalendarGridBuilder.rowIndexFor(LocalTime.of(9, 0));
        List<Response> cell = grid.cellsByDay().get(DayOfWeek.MONDAY).get(row);

        assertEquals(2, cell.size());
        assertTrue(cell.contains(e1));
        assertTrue(cell.contains(e2));
    }

    @Test
    void entriesBeforeGridStartAreReportedAsOutOfRangeNotDropped() {
        Response early = entry(1, DayOfWeek.FRIDAY, "07:00", "08:00", "EARLY", "Dr. X", "1");
        CalendarGrid grid = CalendarGridBuilder.build(List.of(early));

        assertTrue(grid.outOfRangeEntries().contains(early));
        assertTrue(grid.cellsByDay().get(DayOfWeek.FRIDAY).stream().allMatch(List::isEmpty));
    }

    @Test
    void entriesAtOrAfterGridEndAreReportedAsOutOfRange() {
        Response late = entry(1, DayOfWeek.FRIDAY, "20:00", "21:00", "LATE", "Dr. Y", "2");
        CalendarGrid grid = CalendarGridBuilder.build(List.of(late));

        assertTrue(grid.outOfRangeEntries().contains(late));
    }

    @Test
    void gridStartBoundaryIsInclusive() {
        Response onTime = entry(1, DayOfWeek.FRIDAY, "08:00", "09:00", "ONTIME", "Dr. Z", "3");
        CalendarGrid grid = CalendarGridBuilder.build(List.of(onTime));

        assertFalse(grid.outOfRangeEntries().contains(onTime));
        assertEquals(0, CalendarGridBuilder.rowIndexFor(LocalTime.of(8, 0)));
    }

    @Test
    void gridHasTwelveHourlyRowsFromEightToTwenty() {
        CalendarGrid grid = CalendarGridBuilder.build(List.of());
        assertEquals(12, grid.rowStartTimes().size());
        assertEquals(LocalTime.of(8, 0), grid.rowStartTimes().get(0));
        assertEquals(LocalTime.of(19, 0), grid.rowStartTimes().get(11));
    }

    @Test
    void everyDayOfWeekIsPresentEvenWithNoEntries() {
        CalendarGrid grid = CalendarGridBuilder.build(List.of());
        for (DayOfWeek day : DayOfWeek.values()) {
            assertTrue(grid.cellsByDay().containsKey(day));
            assertEquals(12, grid.cellsByDay().get(day).size());
            assertTrue(grid.cellsByDay().get(day).get(0).isEmpty());
        }
        assertTrue(grid.outOfRangeEntries().isEmpty());
    }
}
