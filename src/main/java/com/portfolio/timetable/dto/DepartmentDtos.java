package com.portfolio.timetable.dto;

import jakarta.validation.constraints.NotBlank;

public class DepartmentDtos {

    private DepartmentDtos() {
    }

    public record Request(
            @NotBlank(message = "Department code is required") String code,
            @NotBlank(message = "Department name is required") String name) {
    }

    public record Response(Long id, String code, String name) {
    }
}
