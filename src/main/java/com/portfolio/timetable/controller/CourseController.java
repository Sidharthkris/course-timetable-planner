package com.portfolio.timetable.controller;

import com.portfolio.timetable.dto.CourseDtos.Request;
import com.portfolio.timetable.dto.CourseDtos.Response;
import com.portfolio.timetable.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
@Tag(name = "Courses", description = "Courses offered by a department")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    @Operation(summary = "Create a course (coordinator only)")
    public ResponseEntity<Response> create(@Valid @RequestBody Request request) {
        Response created = courseService.create(request);
        return ResponseEntity.created(URI.create("/api/courses/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "List all courses")
    public List<Response> findAll() {
        return courseService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a course by ID")
    public Response findById(@PathVariable Long id) {
        return courseService.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a course (coordinator only)")
    public Response update(@PathVariable Long id, @Valid @RequestBody Request request) {
        return courseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a course (coordinator only)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        courseService.delete(id);
    }
}
