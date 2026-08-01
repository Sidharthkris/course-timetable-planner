package com.portfolio.timetable.config;

import com.portfolio.timetable.dto.CourseDtos;
import com.portfolio.timetable.dto.DepartmentDtos;
import com.portfolio.timetable.dto.InstructorDtos;
import com.portfolio.timetable.dto.RoomDtos;
import com.portfolio.timetable.dto.ScheduleEntryDtos;
import com.portfolio.timetable.service.CourseService;
import com.portfolio.timetable.service.DepartmentService;
import com.portfolio.timetable.service.InstructorService;
import com.portfolio.timetable.service.RoomService;
import com.portfolio.timetable.service.ScheduleEntryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Populates a handful of departments, instructors, rooms, courses,
 * and a small conflict-free timetable so there's something to look
 * at immediately after starting the app with the {@code dev} profile.
 *
 * <p>This runs at application startup, outside of any HTTP request —
 * there's no logged-in user yet, but the create methods it calls are
 * all guarded by {@code @PreAuthorize("hasRole('COORDINATOR')")}.
 * Rather than weaken that guard, this seeder briefly installs a
 * synthetic "system" authentication with the COORDINATOR role for
 * the duration of the seeding, then clears it — the same pattern a
 * real application would use for a scheduled job or migration script
 * that needs to bypass normal user-based authorization.
 */
@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private final DepartmentService departmentService;
    private final InstructorService instructorService;
    private final RoomService roomService;
    private final CourseService courseService;
    private final ScheduleEntryService scheduleEntryService;

    public DevDataSeeder(DepartmentService departmentService, InstructorService instructorService,
                          RoomService roomService, CourseService courseService,
                          ScheduleEntryService scheduleEntryService) {
        this.departmentService = departmentService;
        this.instructorService = instructorService;
        this.roomService = roomService;
        this.courseService = courseService;
        this.scheduleEntryService = scheduleEntryService;
    }

    @Override
    public void run(String... args) {
        runAsSystemCoordinator(this::seedData);
    }

    private void seedData() {
        DepartmentDtos.Response cs = departmentService.create(new DepartmentDtos.Request("CS", "Computer Science"));
        DepartmentDtos.Response ds = departmentService.create(new DepartmentDtos.Request("DS", "Data Science"));

        InstructorDtos.Response rao = instructorService.create(
                new InstructorDtos.Request("Dr. Rao", "rao@example.edu", cs.id()));
        InstructorDtos.Response iyer = instructorService.create(
                new InstructorDtos.Request("Dr. Iyer", "iyer@example.edu", ds.id()));

        RoomDtos.Response room101 = roomService.create(new RoomDtos.Request("101", "Main Building", 40));
        RoomDtos.Response room202 = roomService.create(new RoomDtos.Request("202", "Annex Building", 25));

        CourseDtos.Response javaCourse = courseService.create(
                new CourseDtos.Request("CS101", "Java Programming", 4, cs.id()));
        CourseDtos.Response dataCourse = courseService.create(
                new CourseDtos.Request("DS101", "Introduction to Data Science", 4, ds.id()));

        scheduleEntryService.create(new ScheduleEntryDtos.Request(
                javaCourse.id(), rao.id(), room101.id(),
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)));
        scheduleEntryService.create(new ScheduleEntryDtos.Request(
                dataCourse.id(), iyer.id(), room202.id(),
                DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)));
        scheduleEntryService.create(new ScheduleEntryDtos.Request(
                javaCourse.id(), rao.id(), room101.id(),
                DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)));

        System.out.println("""

                ---------------------------------------------------------------
                Dev data seeded: 2 departments, 2 instructors, 2 rooms, 2 courses, 3 schedule entries.
                Log in at http://localhost:8080/login as:
                  coordinator / coordinator123  (full access)
                  instructor  / instructor123   (view only)
                Swagger UI: http://localhost:8080/swagger-ui.html
                ---------------------------------------------------------------
                """);
    }

    private void runAsSystemCoordinator(Runnable action) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_COORDINATOR"));
        var systemAuth = new UsernamePasswordAuthenticationToken("system-seeder", null, authorities);
        SecurityContextHolder.getContext().setAuthentication(systemAuth);
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
