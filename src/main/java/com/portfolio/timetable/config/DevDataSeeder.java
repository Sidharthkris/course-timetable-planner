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
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Populates a handful of departments, instructors, rooms, courses,
 * and a small conflict-free timetable so there's something to look
 * at in Swagger UI immediately after starting the app with the
 * {@code dev} profile. Never runs against the production/Docker
 * profile, which is expected to start with an empty database.
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
                Try POSTing another entry for Dr. Rao on Monday 09:00-11:00 in room 101 -> expect 409 Conflict.
                Swagger UI: http://localhost:8080/swagger-ui.html
                ---------------------------------------------------------------
                """);
    }
}
