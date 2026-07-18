package com.portfolio.timetable.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class RoomDtos {

    private RoomDtos() {
    }

    public record Request(
            @NotBlank(message = "Room number is required") String roomNumber,
            String building,
            @Positive(message = "Capacity must be positive") int capacity) {
    }

    public record Response(Long id, String roomNumber, String building, int capacity) {
    }
}
