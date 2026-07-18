package com.portfolio.timetable.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class ScheduleEntryDtos {

    private ScheduleEntryDtos() {
    }

    public record Request(
            @NotNull(message = "courseId is required") Long courseId,
            @NotNull(message = "instructorId is required") Long instructorId,
            @NotNull(message = "roomId is required") Long roomId,
            @NotNull(message = "dayOfWeek is required") DayOfWeek dayOfWeek,
            @NotNull(message = "startTime is required") LocalTime startTime,
            @NotNull(message = "endTime is required") LocalTime endTime) {
    }

    public record Response(
            Long id,
            CourseDtos.Response course,
            InstructorDtos.Response instructor,
            RoomDtos.Response room,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime) {
    }
}
