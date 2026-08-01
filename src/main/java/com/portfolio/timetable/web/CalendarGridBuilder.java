package com.portfolio.timetable.web;

import com.portfolio.timetable.dto.ScheduleEntryDtos.Response;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a weekly calendar grid (day columns × hourly-row time slots)
 * from a flat list of schedule entries. Deliberately has zero Spring
 * or JPA dependency — it's pure {@code java.time} arithmetic over
 * plain DTOs — so it can be unit tested (and was, by hand, outside
 * this codebase) without spinning up any part of the framework.
 *
 * <p>Each grid cell holds a <em>list</em> of entries rather than at
 * most one, and cells are not merged across rows (no HTML
 * {@code rowspan}). Two courses can legitimately run at the same time
 * in different rooms — the conflict detector only forbids the same
 * instructor or room double-booking, not the calendar slot itself —
 * so a cell needs to support more than one entry, and rowspan-based
 * layouts don't handle that cleanly. Each entry's own start/end time
 * is printed in its cell regardless of which row it's anchored to.
 */
public final class CalendarGridBuilder {

    public static final LocalTime GRID_START = LocalTime.of(8, 0);
    public static final LocalTime GRID_END = LocalTime.of(20, 0);

    public static final List<DayOfWeek> DISPLAY_DAYS = List.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    private CalendarGridBuilder() {
    }

    public record CalendarGrid(
            List<LocalTime> rowStartTimes,
            Map<DayOfWeek, List<List<Response>>> cellsByDay,
            List<Response> outOfRangeEntries) {
    }

    public static CalendarGrid build(List<Response> entries) {
        List<LocalTime> rowStartTimes = buildRowStartTimes();
        int rowCount = rowStartTimes.size();

        Map<DayOfWeek, List<List<Response>>> cellsByDay = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DISPLAY_DAYS) {
            List<List<Response>> rows = new ArrayList<>(rowCount);
            for (int i = 0; i < rowCount; i++) {
                rows.add(new ArrayList<>());
            }
            cellsByDay.put(day, rows);
        }

        List<Response> outOfRange = new ArrayList<>();

        List<Response> sorted = entries.stream()
                .sorted(Comparator.comparing(Response::startTime))
                .toList();

        for (Response entry : sorted) {
            int rowIndex = rowIndexFor(entry.startTime());
            List<List<Response>> dayRows = cellsByDay.get(entry.dayOfWeek());
            if (rowIndex < 0 || rowIndex >= rowCount || dayRows == null) {
                outOfRange.add(entry);
                continue;
            }
            dayRows.get(rowIndex).add(entry);
        }

        return new CalendarGrid(rowStartTimes, cellsByDay, outOfRange);
    }

    /** @return the row index for a time within [GRID_START, GRID_END), or -1 if outside that window. */
    public static int rowIndexFor(LocalTime time) {
        if (time.isBefore(GRID_START) || !time.isBefore(GRID_END)) {
            return -1;
        }
        long minutesFromStart = Duration.between(GRID_START, time).toMinutes();
        return (int) (minutesFromStart / 60);
    }

    private static List<LocalTime> buildRowStartTimes() {
        List<LocalTime> rows = new ArrayList<>();
        LocalTime current = GRID_START;
        while (current.isBefore(GRID_END)) {
            rows.add(current);
            current = current.plusHours(1);
        }
        return rows;
    }
}
