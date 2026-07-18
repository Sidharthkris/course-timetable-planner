package com.portfolio.timetable.controller;

import com.portfolio.timetable.dto.ScheduleEntryDtos.Request;
import com.portfolio.timetable.dto.ScheduleEntryDtos.Response;
import com.portfolio.timetable.service.ScheduleEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.DayOfWeek;
import java.util.List;

@RestController
@RequestMapping("/api/schedule-entries")
@Tag(name = "Schedule Entries", description = "The timetable itself: course + instructor + room + day + time, with automatic conflict detection")
public class ScheduleEntryController {

    private final ScheduleEntryService scheduleEntryService;

    public ScheduleEntryController(ScheduleEntryService scheduleEntryService) {
        this.scheduleEntryService = scheduleEntryService;
    }

    @PostMapping
    @Operation(summary = "Create a schedule entry",
            description = "Returns 409 Conflict if the instructor or room is already booked at an overlapping time on the same day")
    public ResponseEntity<Response> create(@Valid @RequestBody Request request) {
        Response created = scheduleEntryService.create(request);
        return ResponseEntity.created(URI.create("/api/schedule-entries/" + created.id())).body(created);
    }

    @PostMapping("/check-conflicts")
    @Operation(summary = "Dry-run a proposed slot",
            description = "Returns the list of entries a proposed slot would conflict with, without saving anything. An empty list means the slot is free.")
    public List<Response> checkConflicts(@Valid @RequestBody Request request) {
        return scheduleEntryService.checkConflicts(request);
    }

    @GetMapping
    @Operation(summary = "List schedule entries, optionally filtered by instructor, room, course, or day of week")
    public Page<Response> search(
            @Parameter(description = "Filter by instructor ID") @RequestParam(required = false) Long instructorId,
            @Parameter(description = "Filter by room ID") @RequestParam(required = false) Long roomId,
            @Parameter(description = "Filter by course ID") @RequestParam(required = false) Long courseId,
            @Parameter(description = "Filter by day of week, e.g. MONDAY") @RequestParam(required = false) DayOfWeek dayOfWeek,
            @PageableDefault(size = 20, sort = "dayOfWeek") Pageable pageable) {
        return scheduleEntryService.search(instructorId, roomId, courseId, dayOfWeek, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a schedule entry by ID")
    public Response findById(@PathVariable Long id) {
        return scheduleEntryService.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a schedule entry",
            description = "Re-runs conflict detection excluding this entry itself; returns 409 if the new slot clashes with a different entry")
    public Response update(@PathVariable Long id, @Valid @RequestBody Request request) {
        return scheduleEntryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a schedule entry")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        scheduleEntryService.delete(id);
    }
}
