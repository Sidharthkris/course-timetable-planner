package com.portfolio.timetable.controller;

import com.portfolio.timetable.dto.RoomDtos.Request;
import com.portfolio.timetable.dto.RoomDtos.Response;
import com.portfolio.timetable.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Rooms", description = "Physical rooms that can be booked for schedule entries")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    @Operation(summary = "Create a room")
    public ResponseEntity<Response> create(@Valid @RequestBody Request request) {
        Response created = roomService.create(request);
        return ResponseEntity.created(URI.create("/api/rooms/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "List all rooms")
    public List<Response> findAll() {
        return roomService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a room by ID")
    public Response findById(@PathVariable Long id) {
        return roomService.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a room")
    public Response update(@PathVariable Long id, @Valid @RequestBody Request request) {
        return roomService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a room")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        roomService.delete(id);
    }
}
