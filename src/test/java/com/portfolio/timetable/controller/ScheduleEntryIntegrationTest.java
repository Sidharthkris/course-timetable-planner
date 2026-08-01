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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack test: real Spring context (including Spring Security),
 * real JPA repositories, an in-memory H2 database, and MockMvc
 * driving actual HTTP requests through the real controllers. Each
 * test runs in its own transaction that's rolled back afterward.
 *
 * <p>{@code @WithMockUser(roles = "COORDINATOR")} at the class level
 * authenticates every {@code mockMvc.perform(...)} call as a
 * coordinator by default, so the existing CRUD/conflict tests don't
 * need to know security exists. The tests that specifically need a
 * different role override it per-method.
 *
 * <p>{@code setUp()} below calls the services directly (not through
 * MockMvc) to build prerequisite data, and {@code @WithMockUser}'s
 * security context isn't guaranteed to be active yet at that exact
 * point in the JUnit 5 lifecycle — so it establishes its own
 * short-lived COORDINATOR context explicitly, the same pattern
 * {@code DevDataSeeder} uses for startup seeding, and restores
 * whatever context existed before it when done.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "test-coordinator", roles = "COORDINATOR")
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
        runAsSystemCoordinator(() -> {
            DepartmentDtos.Response department = departmentService.create(
                    new DepartmentDtos.Request("CS", "Computer Science"));
            instructorId = instructorService.create(
                    new InstructorDtos.Request("Dr. Rao", "rao@example.com", department.id())).id();
            roomId = roomService.create(
                    new RoomDtos.Request("101", "Main Building", 40)).id();
            courseId = courseService.create(
                    new CourseDtos.Request("CS101", "Intro to Programming", 3, department.id())).id();
        });
    }

    private void runAsSystemCoordinator(Runnable action) {
        SecurityContext original = SecurityContextHolder.getContext();
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_COORDINATOR"));
        SecurityContext temporary = SecurityContextHolder.createEmptyContext();
        temporary.setAuthentication(new UsernamePasswordAuthenticationToken("test-setup", null, authorities));
        SecurityContextHolder.setContext(temporary);
        try {
            action.run();
        } finally {
            SecurityContextHolder.setContext(original);
        }
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

    @Test
    @WithMockUser(username = "test-instructor", roles = "INSTRUCTOR")
    void instructorCanReadButCannotCreateScheduleEntries() throws Exception {
        // Read access: allowed for any authenticated user.
        mockMvc.perform(get("/api/schedule-entries"))
                .andExpect(status().isOk());

        // Write access: blocked by @PreAuthorize("hasRole('COORDINATOR')") on the service method.
        mockMvc.perform(post("/api/schedule-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slot(DayOfWeek.MONDAY, "09:00", "10:00"))))
                .andExpect(status().isForbidden());
    }
}
