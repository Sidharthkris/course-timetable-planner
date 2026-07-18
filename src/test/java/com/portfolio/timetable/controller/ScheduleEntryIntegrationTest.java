package com.portfolio.timetable.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.timetable.dto.CourseDtos;
import com.portfolio.timetable.dto.DepartmentDtos;
import com.portfolio.timetable.dto.InstructorDtos;
import com.portfolio.timetable.dto.RoomDtos;
import com.portfolio.timetable.dto.ScheduleEntryDtos.Request;
import com.portfolio.timetable.service.CourseService;
import com.portfolio.timetable.service.DepartmentService;
import com.portfolio.timetable.service.InstructorService;
import com.portfolio.timetable.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack test: real Spring context, real JPA repositories, an
 * in-memory H2 database (see {@code src/test/resources/application.yml}),
 * and MockMvc driving actual HTTP requests through the real
 * controllers. Each test runs in its own transaction that's rolled
 * back afterward, so tests don't interfere with each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ScheduleEntryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private InstructorService instructorService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private CourseService courseService;

    private Long instructorId;
    private Long roomId;
    private Long courseId;

    @BeforeEach
    void setUp() {
        DepartmentDtos.Response department = departmentService.create(
                new DepartmentDtos.Request("CS", "Computer Science"));
        instructorId = instructorService.create(
                new InstructorDtos.Request("Dr. Rao", "rao@example.com", department.id())).id();
        roomId = roomService.create(
                new RoomDtos.Request("101", "Main Building", 40)).id();
        courseId = courseService.create(
                new CourseDtos.Request("CS101", "Intro to Programming", 3, department.id())).id();
    }

    private Request slot(DayOfWeek day, String start, String end) {
        return new Request(courseId, instructorId, roomId, day, LocalTime.parse(start), LocalTime.parse(end));
    }

    @Test
    void createsAScheduleEntrySuccessfully() throws Exception {
        mockMvc.perform(post("/api/schedule-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slot(DayOfWeek.MONDAY, "09:00", "10:00"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.instructor.fullName").value("Dr. Rao"));
    }

    @Test
    void returnsConflictWhenSameInstructorDoubleBooked() throws Exception {
        mockMvc.perform(post("/api/schedule-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slot(DayOfWeek.MONDAY, "09:00", "10:00"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/schedule-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slot(DayOfWeek.MONDAY, "09:30", "10:30"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.conflictingEntries").isArray())
                .andExpect(jsonPath("$.conflictingEntries.length()").value(1));
    }

    @Test
    void allowsBackToBackSlotsForTheSameInstructor() throws Exception {
        mockMvc.perform(post("/api/schedule-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slot(DayOfWeek.MONDAY, "09:00", "10:00"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/schedule-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slot(DayOfWeek.MONDAY, "10:00", "11:00"))))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsInvalidTimeRangeWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/schedule-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slot(DayOfWeek.MONDAY, "10:00", "09:00"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForUnknownScheduleEntry() throws Exception {
        mockMvc.perform(get("/api/schedule-entries/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void filtersScheduleEntriesByInstructorId() throws Exception {
        mockMvc.perform(post("/api/schedule-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slot(DayOfWeek.WEDNESDAY, "13:00", "14:00"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/schedule-entries").param("instructorId", instructorId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }
}
