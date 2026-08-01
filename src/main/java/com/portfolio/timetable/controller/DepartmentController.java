package com.portfolio.timetable.controller;

import com.portfolio.timetable.dto.DepartmentDtos.Request;
import com.portfolio.timetable.dto.DepartmentDtos.Response;
import com.portfolio.timetable.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/departments")
@Tag(name = "Departments", description = "Academic departments courses and instructors belong to")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping
    @Operation(summary = "Create a department (coordinator only)")
    public ResponseEntity<Response> create(@Valid @RequestBody Request request) {
        Response created = departmentService.create(request);
        return ResponseEntity.created(URI.create("/api/departments/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "List all departments")
    public List<Response> findAll() {
        return departmentService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a department by ID")
    public Response findById(@PathVariable Long id) {
        return departmentService.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a department (coordinator only)")
    public Response update(@PathVariable Long id, @Valid @RequestBody Request request) {
        return departmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a department (coordinator only)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        departmentService.delete(id);
    }
}
