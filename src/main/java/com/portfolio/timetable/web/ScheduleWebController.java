package com.portfolio.timetable.web;

import com.portfolio.timetable.dto.ScheduleEntryDtos.Request;
import com.portfolio.timetable.exception.ResourceNotFoundException;
import com.portfolio.timetable.exception.ScheduleConflictException;
import com.portfolio.timetable.service.CourseService;
import com.portfolio.timetable.service.InstructorService;
import com.portfolio.timetable.service.RoomService;
import com.portfolio.timetable.service.ScheduleEntryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * The main GUI page: a weekly calendar grid (day columns x hourly
 * rows, see {@link CalendarGridBuilder}) plus a create form. The REST
 * API's {@code GET /api/schedule-entries} supports full
 * pagination/filtering; this page keeps things simple and lays out up
 * to 200 entries at once, which is plenty for a demo-scale dataset.
 */
@Controller
@RequestMapping("/schedule")
public class ScheduleWebController {

    private static final int MAX_ENTRIES_SHOWN = 200;

    private final ScheduleEntryService scheduleEntryService;
    private final CourseService courseService;
    private final InstructorService instructorService;
    private final RoomService roomService;

    public ScheduleWebController(ScheduleEntryService scheduleEntryService, CourseService courseService,
                                  InstructorService instructorService, RoomService roomService) {
        this.scheduleEntryService = scheduleEntryService;
        this.courseService = courseService;
        this.instructorService = instructorService;
        this.roomService = roomService;
    }

    @GetMapping
    public String list(Model model) {
        var pageable = PageRequest.of(0, MAX_ENTRIES_SHOWN, Sort.by("dayOfWeek").and(Sort.by("startTime")));
        var entries = scheduleEntryService.search(null, null, null, null, pageable).getContent();

        model.addAttribute("grid", CalendarGridBuilder.build(entries));
        model.addAttribute("displayDays", CalendarGridBuilder.DISPLAY_DAYS);
        model.addAttribute("courses", courseService.findAll());
        model.addAttribute("instructors", instructorService.findAll());
        model.addAttribute("rooms", roomService.findAll());
        return "schedule";
    }

    @PostMapping
    public String create(@RequestParam Long courseId, @RequestParam Long instructorId, @RequestParam Long roomId,
                          @RequestParam DayOfWeek dayOfWeek, @RequestParam LocalTime startTime,
                          @RequestParam LocalTime endTime, RedirectAttributes redirectAttributes) {
        try {
            scheduleEntryService.create(new Request(courseId, instructorId, roomId, dayOfWeek, startTime, endTime));
            redirectAttributes.addFlashAttribute("successMessage", "Schedule entry created.");
        } catch (ScheduleConflictException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (IllegalArgumentException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/schedule";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            scheduleEntryService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Schedule entry deleted.");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/schedule";
    }
}
