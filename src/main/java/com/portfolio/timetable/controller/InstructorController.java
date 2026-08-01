package com.portfolio.timetable.controller;

import com.portfolio.timetable.dto.InstructorDtos.Request;
import com.portfolio.timetable.dto.InstructorDtos.Response;
import com.portfolio.timetable.service.InstructorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/instructors")
@Tag(name = "Instructors", description = "Teaching staff who can be assigned to schedule entries")
public class InstructorController {

    private final InstructorService instructorService;

    public InstructorController(InstructorService instructorService) {
        this.instructorService = instructorService;
    }

    @PostMapping
    @Operation(summary = "Create an instructor (coordinator only)")
    public ResponseEntity<Response> create(@Valid @RequestBody Request request) {
        Response created = instructorService.create(request);
        return ResponseEntity.created(URI.create("/api/instructors/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "List all instructors")
    public List<Response> findAll() {
        return instructorService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an instructor by ID")
    public Response findById(@PathVariable Long id) {
        return instructorService.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an instructor (coordinator only)")
    public Response update(@PathVariable Long id, @Valid @RequestBody Request request) {
        return instructorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an instructor (coordinator only)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        instructorService.delete(id);
    }
}
